package com.example.ui

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.CloneCatalog
import com.example.data.CloneEngineType
import com.example.data.ClonedSpace
import com.example.data.ClonedSpaceRepository
import com.example.data.DualAppPreset
import com.example.data.DualDockDatabase
import com.example.data.SecurityPreferences
import com.example.data.SecuritySettings
import com.example.system.BiometricAuthHelper
import com.example.system.BiometricAvailability
import com.example.system.CloneSlotLauncher
import com.example.system.InstalledAppInfo
import com.example.system.InstalledAppsBridge
import com.example.system.ShortcutHelper
import com.example.system.ShortcutPinResult
import com.example.system.WorkProfileAppInfo
import com.example.system.WorkProfileManager
import com.example.system.WorkProfileStatus
import com.example.ui.theme.BadgeSwatches
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DualDockTab(val label: String) {
    VAULT("Vault"),
    APPS("Clone"),
    WORK_SPACE("Work OS"),
    SECURITY("Security")
}

enum class VaultFilter(val label: String) {
    ALL("All"),
    CONTAINERS("Containers"),
    WORK("Work OS"),
    LOCKED("Locked")
}

data class CloneConfigDraft(
    val editingId: Long? = null,
    val engineType: String = CloneEngineType.WEB_CONTAINER,
    val existingSlotIndex: Int = 0,
    val displayName: String = "",
    val targetUrl: String = "",
    val packageName: String = "",
    val presetKey: String? = null,
    val badgeText: String = "2",
    val badgeColorHex: Long = 0xFF8B5CF6,
    val useDesktopUserAgent: Boolean = false,
    val isLocked: Boolean = false,
    val pinShortcutOnSave: Boolean = false,
    val batchCount: Int = 1, // 1, 2, 3, or 5 clones at once!
    val helperHint: String? = null
)

data class DualDockUiState(
    val selectedTab: DualDockTab = DualDockTab.VAULT,
    val vaultFilter: VaultFilter = VaultFilter.ALL,
    val appSearchQuery: String = "",
    val selectedPresetCategory: String = "All",
    val clonedSpaces: List<ClonedSpace> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = true,
    val workProfileStatus: WorkProfileStatus = WorkProfileStatus(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
    val isShortcutPinSupported: Boolean = true,
    val isVaultLocked: Boolean = false,
    val pendingUnlockSpace: ClonedSpace? = null,
    val lockErrorMessage: String? = null,
    val activeConfigDraft: CloneConfigDraft? = null,
    val selectedActionsSpace: ClonedSpace? = null,
    val showPinSetupSheet: Boolean = false,
    val pinSetupPendingSpaceId: Long? = null,
    val hasCompletedLaunchIntro: Boolean = false
) {
    val totalClonesCount: Int
        get() = clonedSpaces.size

    val lockedClonesCount: Int
        get() = clonedSpaces.count { it.isLocked }

    val filteredSpaces: List<ClonedSpace>
        get() = when (vaultFilter) {
            VaultFilter.ALL -> clonedSpaces
            VaultFilter.CONTAINERS -> clonedSpaces.filter { it.isContainer }
            VaultFilter.WORK -> clonedSpaces.filter { it.isWorkProfile }
            VaultFilter.LOCKED -> clonedSpaces.filter { it.isLocked }
        }

    fun cloneCountForPreset(presetKey: String): Int =
        clonedSpaces.count { it.presetKey == presetKey }
}

sealed class DualDockEvent {
    data class ShowMessage(val text: String) : DualDockEvent()
    data object RequestWorkProfileProvisioning : DualDockEvent()
    data class RequestBiometricPrompt(val title: String, val subtitle: String) : DualDockEvent()
}

class DualDockViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = ClonedSpaceRepository(
        DualDockDatabase.getInstance(appContext).clonedSpaceDao()
    )
    private val securityPrefs = SecurityPreferences(appContext)
    val installedAppsBridge = InstalledAppsBridge(appContext)
    val workProfileManager = WorkProfileManager(appContext)

    private val _uiState = MutableStateFlow(DualDockUiState())
    val uiState: StateFlow<DualDockUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DualDockEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DualDockEvent> = _events.asSharedFlow()

    private val safeExceptionHandler = CoroutineExceptionHandler { _, _ ->
        _uiState.update { it.copy(isLoadingApps = false) }
    }

    private var initialLockEvaluated = false
    private var lastBackgroundTimestampMs: Long = 0L
    private var lastDeviceRefreshMs: Long = 0L

    init {
        viewModelScope.launch(safeExceptionHandler) {
            combine(
                repository.allSpaces,
                securityPrefs.settingsFlow
            ) { spaces, security ->
                spaces to security
            }.collect { (spaces, security) ->
                val shouldLockOnStart = !initialLockEvaluated && security.isVaultLockEnabled && security.hasPin
                initialLockEvaluated = true
                _uiState.update { current ->
                    current.copy(
                        clonedSpaces = spaces,
                        securitySettings = security,
                        isVaultLocked = if (shouldLockOnStart) true else current.isVaultLocked && security.hasPin
                    )
                }
                // Pre-warm cloned app icons on IO thread so the icon grid renders without pop-in or jank.
                val pkgsToWarm = buildList {
                    spaces.forEach { if (it.packageName.isNotBlank()) add(it.packageName) }
                    add("com.myntra.android")
                    add("com.flipkart.android")
                    add("com.whatsapp")
                }
                installedAppsBridge.prewarmIcons(pkgsToWarm)
            }
        }
        refreshDeviceState(force = true)
    }

    fun onLaunchIntroFinished() {
        _uiState.update { it.copy(hasCompletedLaunchIntro = true) }
    }

    fun refreshDeviceState(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val alreadyPopulated = _uiState.value.installedApps.isNotEmpty()
        if (!force && alreadyPopulated && (now - lastDeviceRefreshMs) < 30_000L) {
            return
        }
        lastDeviceRefreshMs = now
        if (force) {
            installedAppsBridge.clearMissingCache()
        }
        viewModelScope.launch(safeExceptionHandler) {
            if (!alreadyPopulated) {
                _uiState.update { it.copy(isLoadingApps = true) }
            }
            val apps = installedAppsBridge.loadInstalledLaunchableApps()
            val workStatus = withContext(Dispatchers.IO) {
                runCatching { workProfileManager.inspectStatus() }.getOrDefault(WorkProfileStatus())
            }
            val bioAvail = withContext(Dispatchers.IO) {
                runCatching { BiometricAuthHelper.checkAvailability(appContext) }
                    .getOrDefault(BiometricAvailability.UNAVAILABLE)
            }
            val canPin = withContext(Dispatchers.IO) {
                runCatching { ShortcutHelper.isPinningSupported(appContext) }.getOrDefault(true)
            }
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    isLoadingApps = false,
                    workProfileStatus = workStatus,
                    biometricAvailability = bioAvail,
                    isShortcutPinSupported = canPin
                )
            }
        }
    }

    fun selectTab(tab: DualDockTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectVaultFilter(filter: VaultFilter) {
        _uiState.update { it.copy(vaultFilter = filter) }
    }

    fun selectPresetCategory(category: String) {
        _uiState.update { it.copy(selectedPresetCategory = category) }
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun getAppIcon(packageName: String): Drawable? = installedAppsBridge.getAppIcon(packageName)

    fun getCachedIconBitmap(packageName: String): ImageBitmap? =
        installedAppsBridge.getCachedIconBitmap(packageName)

    suspend fun loadIconBitmap(packageName: String): ImageBitmap? =
        installedAppsBridge.loadIconBitmap(packageName)

    fun openPresetCloneConfig(preset: DualAppPreset, matchedInstalledPkg: String? = null) {
        val existingCount = _uiState.value.clonedSpaces.count {
            it.presetKey == preset.key || it.displayName.startsWith(preset.name, ignoreCase = true)
        }
        val nextNum = existingCount + 2
        val swatch = BadgeSwatches[existingCount % BadgeSwatches.size].second

        _uiState.update {
            it.copy(
                activeConfigDraft = CloneConfigDraft(
                    engineType = CloneEngineType.WEB_CONTAINER,
                    displayName = "${preset.name} $nextNum",
                    targetUrl = preset.url,
                    packageName = matchedInstalledPkg ?: preset.knownPackageNames.firstOrNull().orEmpty(),
                    presetKey = preset.key,
                    badgeText = nextNum.toString().take(2),
                    badgeColorHex = swatch,
                    useDesktopUserAgent = preset.defaultDesktopUa,
                    batchCount = 1,
                    helperHint = preset.helperHint
                )
            )
        }
    }

    fun openCustomContainerConfig() {
        val count = _uiState.value.clonedSpaces.size
        val nextNum = count + 1
        _uiState.update {
            it.copy(
                activeConfigDraft = CloneConfigDraft(
                    engineType = CloneEngineType.WEB_CONTAINER,
                    displayName = "Private Space #$nextNum",
                    targetUrl = "https://",
                    badgeText = nextNum.toString().take(2),
                    badgeColorHex = BadgeSwatches[count % BadgeSwatches.size].second,
                    batchCount = 1,
                    helperHint = "Enter any shopping, messaging, or web app URL to create unlimited isolated accounts."
                )
            )
        }
    }

    fun openInstalledAppCloneConfig(app: InstalledAppInfo) {
        val preset = CloneCatalog.findByKey(app.matchedPresetKey)
        if (preset != null) {
            openPresetCloneConfig(preset, matchedInstalledPkg = app.packageName)
            return
        }

        val workStatus = _uiState.value.workProfileStatus
        val existingInWork = workStatus.workApps.any { it.packageName == app.packageName }
        if (existingInWork) {
            _uiState.update {
                it.copy(
                    activeConfigDraft = CloneConfigDraft(
                        engineType = CloneEngineType.WORK_PROFILE,
                        displayName = "${app.label} (Work)",
                        packageName = app.packageName,
                        badgeText = "W",
                        badgeColorHex = 0xFF38BDF8,
                        batchCount = 1,
                        helperHint = "Found in Android Work Profile. Add to DualDock for quick launch, privacy lock, and Home Screen shortcuts."
                    )
                )
            }
        } else {
            val count = _uiState.value.clonedSpaces.size
            val nextNum = count + 2
            _uiState.update {
                it.copy(
                    activeConfigDraft = CloneConfigDraft(
                        engineType = CloneEngineType.WEB_CONTAINER,
                        displayName = "${app.label} $nextNum",
                        targetUrl = "https://www.google.com/search?q=${app.label.replace(" ", "+")}+login",
                        packageName = app.packageName,
                        badgeText = nextNum.toString().take(2),
                        badgeColorHex = BadgeSwatches[count % BadgeSwatches.size].second,
                        batchCount = 1,
                        helperHint = "Enter the web login URL for ${app.label} to create unlimited isolated accounts, or set up Native Work OS in the Work OS tab."
                    )
                )
            }
        }
    }

    fun openWorkAppImportConfig(workApp: WorkProfileAppInfo) {
        val preset = CloneCatalog.findByPackageName(workApp.packageName)
        _uiState.update {
            it.copy(
                activeConfigDraft = CloneConfigDraft(
                    engineType = CloneEngineType.WORK_PROFILE,
                    displayName = "${workApp.label} (Work)",
                    packageName = workApp.packageName,
                    presetKey = preset?.key,
                    badgeText = "W",
                    badgeColorHex = 0xFF38BDF8,
                    batchCount = 1,
                    helperHint = "Native OS Work Profile instance — completely separated from your personal app data."
                )
            )
        }
    }

    fun openEditCloneConfig(space: ClonedSpace) {
        _uiState.update {
            it.copy(
                selectedActionsSpace = null,
                activeConfigDraft = CloneConfigDraft(
                    editingId = space.id,
                    engineType = space.engineType,
                    existingSlotIndex = space.slotIndex,
                    displayName = space.displayName,
                    targetUrl = space.targetUrl,
                    packageName = space.packageName,
                    presetKey = space.presetKey,
                    badgeText = space.badgeText,
                    badgeColorHex = space.badgeColorHex,
                    useDesktopUserAgent = space.useDesktopUserAgent,
                    isLocked = space.isLocked,
                    batchCount = 1
                )
            )
        }
    }

    fun dismissConfigDraft() {
        _uiState.update { it.copy(activeConfigDraft = null) }
    }

    /**
     * 1-Tap Duplicate: Instantly creates another independent isolated clone of an existing space
     * with auto-incremented numbering and a fresh vibrant badge accent!
     */
    fun duplicateClone(source: ClonedSpace) {
        _uiState.update { it.copy(selectedActionsSpace = null) }
        viewModelScope.launch {
            val baseName = source.displayName.replace(Regex("\\s*#?\\d+$"), "").trim()
            val similarCount = _uiState.value.clonedSpaces.count {
                (source.presetKey != null && it.presetKey == source.presetKey) ||
                    it.displayName.startsWith(baseName, ignoreCase = true)
            }
            val nextNumber = similarCount + 2
            val nextSlot = if (source.isContainer) repository.allocateSlotOrUnlimited() else 0
            val nextColor = BadgeSwatches[(similarCount + 1) % BadgeSwatches.size].second

            val duplicated = source.copy(
                id = 0L,
                displayName = "$baseName $nextNumber",
                slotIndex = nextSlot,
                badgeText = nextNumber.toString().take(2),
                badgeColorHex = nextColor,
                createdAt = System.currentTimeMillis(),
                lastLaunchedAt = 0L
            )
            repository.insert(duplicated)
            emitMessage("Created ${duplicated.displayName} (${duplicated.slotLabel})")
        }
    }

    fun saveCloneDraft(draft: CloneConfigDraft) {
        val cleanName = draft.displayName.trim()
        if (cleanName.isEmpty()) {
            emitMessage("Please enter a name for this clone.")
            return
        }

        val cleanUrl = draft.targetUrl.trim()
        if (draft.engineType == CloneEngineType.WEB_CONTAINER &&
            (cleanUrl.isEmpty() || cleanUrl == "https://" || cleanUrl == "http://")
        ) {
            emitMessage("Please enter a valid web address for the container.")
            return
        }

        viewModelScope.launch {
            val finalBadge = draft.badgeText.trim().take(2).ifEmpty { "2" }

            if (draft.editingId != null) {
                val existing = repository.getById(draft.editingId)
                if (existing != null) {
                    val updated = existing.copy(
                        displayName = cleanName,
                        targetUrl = if (draft.engineType == CloneEngineType.WEB_CONTAINER) normalizeUrl(cleanUrl) else existing.targetUrl,
                        badgeText = finalBadge,
                        badgeColorHex = draft.badgeColorHex,
                        useDesktopUserAgent = draft.useDesktopUserAgent,
                        isLocked = draft.isLocked && _uiState.value.securitySettings.hasPin
                    )
                    repository.update(updated)
                    _uiState.update { it.copy(activeConfigDraft = null) }
                    emitMessage("Updated $cleanName")
                    if (draft.pinShortcutOnSave) {
                        pinShortcutForSpace(updated)
                    }
                }
                return@launch
            }

            val totalToCreate = if (draft.engineType == CloneEngineType.WEB_CONTAINER) {
                draft.batchCount.coerceIn(1, 10)
            } else {
                1
            }

            val wantsLock = draft.isLocked
            val hasPin = _uiState.value.securitySettings.hasPin
            val baseName = cleanName.replace(Regex("\\s*#?\\d+$"), "").trim()
            val startNum = Regex("#?(\\d+)$").find(cleanName)?.groupValues?.get(1)?.toIntOrNull()
                ?: (finalBadge.toIntOrNull() ?: 2)

            var firstSaved: ClonedSpace? = null

            for (offset in 0 until totalToCreate) {
                val assignedSlot = if (draft.engineType == CloneEngineType.WEB_CONTAINER) {
                    repository.allocateSlotOrUnlimited()
                } else {
                    0
                }
                val itemName = if (totalToCreate == 1) cleanName else "$baseName ${startNum + offset}"
                val itemBadge = if (totalToCreate == 1) finalBadge else (startNum + offset).toString().take(2)
                val itemColor = if (offset == 0) {
                    draft.badgeColorHex
                } else {
                    BadgeSwatches[(BadgeSwatches.indexOfFirst { it.second == draft.badgeColorHex }.coerceAtLeast(0) + offset) % BadgeSwatches.size].second
                }

                val newSpace = ClonedSpace(
                    displayName = itemName,
                    engineType = draft.engineType,
                    slotIndex = assignedSlot,
                    targetUrl = if (draft.engineType == CloneEngineType.WEB_CONTAINER) normalizeUrl(cleanUrl) else "",
                    packageName = draft.packageName,
                    presetKey = draft.presetKey,
                    badgeText = itemBadge,
                    badgeColorHex = itemColor,
                    useDesktopUserAgent = draft.useDesktopUserAgent,
                    isLocked = wantsLock && hasPin,
                    createdAt = System.currentTimeMillis() + offset
                )

                val insertedId = repository.insert(newSpace)
                if (firstSaved == null) {
                    firstSaved = newSpace.copy(id = insertedId)
                }
            }

            _uiState.update {
                it.copy(
                    activeConfigDraft = null,
                    selectedTab = DualDockTab.VAULT
                )
            }

            val savedPrimary = firstSaved
            if (wantsLock && !hasPin && savedPrimary != null) {
                _uiState.update {
                    it.copy(showPinSetupSheet = true, pinSetupPendingSpaceId = savedPrimary.id)
                }
                emitMessage("Created ${if (totalToCreate > 1) "$totalToCreate clones" else savedPrimary.displayName}. Set a Vault PIN to activate lock.")
            } else if (totalToCreate > 1) {
                emitMessage("Created $totalToCreate isolated clones of $baseName")
            } else if (savedPrimary != null) {
                emitMessage("Created ${savedPrimary.displayName} (${savedPrimary.slotLabel})")
            }

            if (draft.pinShortcutOnSave && savedPrimary != null) {
                pinShortcutForSpace(savedPrimary)
            }
        }
    }

    fun openCloneActions(space: ClonedSpace) {
        _uiState.update { it.copy(selectedActionsSpace = space) }
    }

    fun dismissCloneActions() {
        _uiState.update { it.copy(selectedActionsSpace = null) }
    }

    fun requestLaunchSpace(space: ClonedSpace, clearSession: Boolean = false) {
        _uiState.update { it.copy(selectedActionsSpace = null) }
        val security = _uiState.value.securitySettings
        if (space.isLocked && security.hasPin) {
            _uiState.update {
                it.copy(
                    pendingUnlockSpace = space,
                    lockErrorMessage = null
                )
            }
            if (security.isBiometricEnabled &&
                _uiState.value.biometricAvailability == BiometricAvailability.AVAILABLE
            ) {
                _events.tryEmit(
                    DualDockEvent.RequestBiometricPrompt(
                        title = "Unlock ${space.displayName}",
                        subtitle = "Verify your identity to launch ${space.slotLabel}"
                    )
                )
            }
            return
        }
        executeLaunchSpace(space, clearSession)
    }

    private fun executeLaunchSpace(space: ClonedSpace, clearSession: Boolean = false) {
        viewModelScope.launch {
            repository.recordLaunch(space.id)
            val launched = CloneSlotLauncher.launchSpace(
                context = appContext,
                space = space,
                workProfileManager = workProfileManager,
                clearSessionOnOpen = clearSession
            )
            if (!launched) {
                emitMessage("Could not launch ${space.displayName}. Verify Work Profile is active.")
            } else if (clearSession) {
                emitMessage("Cleared session for ${space.displayName}")
            }
        }
    }

    fun pinShortcutForSpace(space: ClonedSpace) {
        _uiState.update { it.copy(selectedActionsSpace = null) }
        val iconDrawable = installedAppsBridge.getAppIcon(space.packageName)
        when (val result = ShortcutHelper.pinCloneShortcut(appContext, space, iconDrawable)) {
            ShortcutPinResult.Requested ->
                emitMessage("Shortcut request sent for ${space.displayName}")
            ShortcutPinResult.NotSupported ->
                emitMessage("Your current launcher does not support pinned shortcuts.")
            is ShortcutPinResult.Failed ->
                emitMessage(result.reason)
        }
    }

    fun toggleCloneLock(space: ClonedSpace) {
        _uiState.update { it.copy(selectedActionsSpace = null) }
        val hasPin = _uiState.value.securitySettings.hasPin
        if (!space.isLocked && !hasPin) {
            _uiState.update {
                it.copy(showPinSetupSheet = true, pinSetupPendingSpaceId = space.id)
            }
            return
        }
        viewModelScope.launch {
            val nextLocked = !space.isLocked
            repository.setLocked(space.id, nextLocked)
            emitMessage(
                if (nextLocked) "${space.displayName} is now PIN-locked"
                else "Removed lock from ${space.displayName}"
            )
        }
    }

    fun deleteClone(space: ClonedSpace) {
        _uiState.update { it.copy(selectedActionsSpace = null) }
        viewModelScope.launch {
            repository.delete(space.id)
            emitMessage("Removed ${space.displayName}")
        }
    }

    // Security & Lock Operations
    fun openPinSetupSheet() {
        _uiState.update { it.copy(showPinSetupSheet = true, pinSetupPendingSpaceId = null) }
    }

    fun dismissPinSetupSheet() {
        _uiState.update { it.copy(showPinSetupSheet = false, pinSetupPendingSpaceId = null) }
    }

    fun saveNewPin(pin: String) {
        viewModelScope.launch {
            val pendingSpaceId = _uiState.value.pinSetupPendingSpaceId
            securityPrefs.savePin(pin, enableVaultLock = true)
            if (pendingSpaceId != null) {
                repository.setLocked(pendingSpaceId, true)
            }
            _uiState.update {
                it.copy(
                    showPinSetupSheet = false,
                    pinSetupPendingSpaceId = null
                )
            }
            emitMessage("Vault PIN saved and privacy lock enabled")
        }
    }

    fun toggleVaultLockMaster(enabled: Boolean) {
        if (enabled && !_uiState.value.securitySettings.hasPin) {
            openPinSetupSheet()
            return
        }
        viewModelScope.launch {
            securityPrefs.setVaultLockEnabled(enabled)
            emitMessage(if (enabled) "Vault Lock enabled" else "Vault Lock paused")
        }
    }

    fun toggleBiometricUnlock(enabled: Boolean) {
        if (enabled && !_uiState.value.securitySettings.hasPin) {
            openPinSetupSheet()
            return
        }
        if (enabled && _uiState.value.biometricAvailability != BiometricAvailability.AVAILABLE) {
            emitMessage("Enroll a fingerprint or face unlock in Android Settings first.")
            return
        }
        viewModelScope.launch {
            securityPrefs.setBiometricEnabled(enabled)
            emitMessage(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        viewModelScope.launch {
            securityPrefs.setAutoLockTimeout(seconds)
        }
    }

    fun removeVaultPin() {
        viewModelScope.launch {
            securityPrefs.removePin()
            _uiState.update {
                it.copy(isVaultLocked = false, pendingUnlockSpace = null)
            }
            emitMessage("Vault PIN removed")
        }
    }

    fun lockVaultNow() {
        if (!_uiState.value.securitySettings.hasPin) {
            openPinSetupSheet()
            return
        }
        _uiState.update {
            it.copy(isVaultLocked = true, lockErrorMessage = null)
        }
    }

    fun verifyUnlockPin(candidatePin: String) {
        viewModelScope.launch {
            val valid = securityPrefs.verifyPin(candidatePin)
            if (valid) {
                completeUnlock()
            } else {
                _uiState.update { it.copy(lockErrorMessage = "Incorrect PIN. Try again.") }
            }
        }
    }

    fun onBiometricAuthSucceeded() {
        completeUnlock()
    }

    fun onBiometricAuthError(message: String) {
        _uiState.update { it.copy(lockErrorMessage = message) }
    }

    fun cancelPendingSpaceUnlock() {
        _uiState.update { it.copy(pendingUnlockSpace = null, lockErrorMessage = null) }
    }

    private fun completeUnlock() {
        val targetSpace = _uiState.value.pendingUnlockSpace
        _uiState.update {
            it.copy(
                isVaultLocked = false,
                pendingUnlockSpace = null,
                lockErrorMessage = null
            )
        }
        if (targetSpace != null) {
            executeLaunchSpace(targetSpace)
        }
    }

    fun triggerBiometricUnlock() {
        val pending = _uiState.value.pendingUnlockSpace
        val title = if (pending != null) "Unlock ${pending.displayName}" else "Unlock DualDock"
        val subtitle = if (pending != null) {
            "Verify your identity to open ${pending.slotLabel}"
        } else {
            "Verify your identity to access DualDock Vault"
        }
        _events.tryEmit(DualDockEvent.RequestBiometricPrompt(title, subtitle))
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestampMs = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val security = _uiState.value.securitySettings
        if (!security.isVaultLockEnabled || !security.hasPin) return
        if (lastBackgroundTimestampMs == 0L) return

        val timeoutSec = security.autoLockTimeoutSeconds
        if (timeoutSec < 0) return

        val elapsedSec = (System.currentTimeMillis() - lastBackgroundTimestampMs) / 1000L
        if (elapsedSec >= timeoutSec) {
            _uiState.update { it.copy(isVaultLocked = true, lockErrorMessage = null) }
        }
        lastBackgroundTimestampMs = 0L
    }

    // Work Profile Actions
    fun requestWorkProfileSetup() {
        if (!workProfileManager.canLaunchProvisioning()) {
            emitMessage("Work Profile provisioning is not available on this device. Use Unlimited Instant Containers instead.")
            return
        }
        _events.tryEmit(DualDockEvent.RequestWorkProfileProvisioning)
    }

    fun openWorkPlayStore() {
        if (!workProfileManager.openWorkPlayStore()) {
            emitMessage("Work Play Store is not available in this profile.")
        }
    }

    fun openDeviceAccountSettings() {
        workProfileManager.openAccountSettings()
    }

    fun launchWorkProfileAppDirect(workApp: WorkProfileAppInfo) {
        if (!workProfileManager.launchWorkApp(workApp.packageName)) {
            emitMessage("Unable to launch ${workApp.label} in Work Profile.")
        }
    }

    fun openNativeAppViaWorkSpace(appName: String, packageName: String) {
        _uiState.update { it.copy(activeConfigDraft = null) }
        when (workProfileManager.openAppInWorkOrPlayStore(packageName)) {
            com.example.system.WorkNativeLaunchResult.LAUNCHED_NATIVE_APP -> {
                emitMessage("Opened native $appName in Work Space")
            }
            com.example.system.WorkNativeLaunchResult.OPENED_WORK_PLAY_STORE -> {
                emitMessage("Install $appName in Work Play Store for native WhatsApp OTP, then import it in Work Space.")
            }
            com.example.system.WorkNativeLaunchResult.NEEDS_WORK_PROFILE -> {
                _uiState.update { it.copy(selectedTab = DualDockTab.WORK_SPACE) }
                emitMessage("Create a Work Profile once to run the native $appName app with WhatsApp OTP.")
            }
            com.example.system.WorkNativeLaunchResult.WORK_PLAY_STORE_UNAVAILABLE -> {
                _uiState.update { it.copy(selectedTab = DualDockTab.WORK_SPACE) }
                emitMessage("Open Work Space to manage your native Work Profile apps.")
            }
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun emitMessage(text: String) {
        _events.tryEmit(DualDockEvent.ShowMessage(text))
    }
}
