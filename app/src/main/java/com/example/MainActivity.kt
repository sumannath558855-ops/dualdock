package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.system.BiometricAuthHelper
import com.example.system.BiometricAvailability
import com.example.ui.DualDockEvent
import com.example.ui.DualDockTab
import com.example.ui.DualDockUiState
import com.example.ui.DualDockViewModel
import com.example.ui.components.CloneActionsBottomSheet
import com.example.ui.components.CloneConfigBottomSheet
import com.example.ui.components.PinSetupBottomSheet
import com.example.ui.components.rememberAsyncAppIcon
import com.example.ui.screens.AppsPickerScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.VaultLockScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.WorkSpaceScreen
import com.example.ui.theme.CanvasWhite
import com.example.ui.theme.DeepLavender
import com.example.ui.theme.DualDockTheme
import com.example.ui.theme.JetInk
import com.example.ui.theme.LavenderTint
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
    }

    private val viewModel: DualDockViewModel by viewModels()

    private val workProfileProvisionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDeviceState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        handleInitialTabIntent(intent)

        setContent {
            DualDockTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is DualDockEvent.ShowMessage -> {
                                snackbarHostState.showSnackbar(event.text)
                            }
                            DualDockEvent.RequestWorkProfileProvisioning -> {
                                try {
                                    workProfileProvisionLauncher.launch(
                                        viewModel.workProfileManager.buildProvisioningIntent()
                                    )
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(
                                        "Work Profile setup is restricted on this device. Use Unlimited Clone Studio instead."
                                    )
                                }
                            }
                            is DualDockEvent.RequestBiometricPrompt -> {
                                BiometricAuthHelper.authenticate(
                                    activity = this@MainActivity,
                                    title = event.title,
                                    subtitle = event.subtitle,
                                    onSuccess = { viewModel.onBiometricAuthSucceeded() },
                                    onError = { msg -> viewModel.onBiometricAuthError(msg) }
                                )
                            }
                        }
                    }
                }

                DualDockRootShell(
                    state = state,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInitialTabIntent(intent)
    }

    private fun handleInitialTabIntent(intent: Intent?) {
        val tabName = intent?.getStringExtra(EXTRA_INITIAL_TAB) ?: return
        val tab = DualDockTab.entries.firstOrNull { it.name.equals(tabName, ignoreCase = true) }
            ?: return
        viewModel.selectTab(tab)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
        viewModel.refreshDeviceState(force = false)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }
}

private data class NavDestination(
    val tab: DualDockTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val dualDockDestinations = listOf(
    NavDestination(DualDockTab.VAULT, Icons.Rounded.Home, Icons.Outlined.Home),
    NavDestination(DualDockTab.APPS, Icons.Rounded.GridView, Icons.Outlined.GridView),
    NavDestination(DualDockTab.WORK_SPACE, Icons.Rounded.Work, Icons.Outlined.WorkOutline),
    NavDestination(DualDockTab.SECURITY, Icons.Rounded.Shield, Icons.Outlined.Shield)
)

@Composable
private fun DualDockRootShell(
    state: DualDockUiState,
    viewModel: DualDockViewModel,
    snackbarHostState: SnackbarHostState
) {
    val isLockedOverlayVisible = state.isVaultLocked || state.pendingUnlockSpace != null

    BackHandler(enabled = !isLockedOverlayVisible && state.selectedTab != DualDockTab.VAULT) {
        viewModel.selectTab(DualDockTab.VAULT)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasWhite)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        contentColor = JetInk,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        dualDockDestinations.forEach { dest ->
                            val selected = state.selectedTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.tab.label
                                    )
                                },
                                label = { Text(dest.tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = JetInk,
                                    selectedTextColor = JetInk,
                                    indicatorColor = LavenderTint,
                                    unselectedIconColor = SlateInk,
                                    unselectedTextColor = SlateInk
                                ),
                                modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.navigationBarsPadding()
                        )
                    },
                    bottomBar = {
                        if (!useNavigationRail) {
                            JetPillFloatingNavBar(
                                selectedTab = state.selectedTab,
                                onSelectTab = viewModel::selectTab
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)

                        Crossfade(
                            targetState = state.selectedTab,
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                            label = "dualdock_tab_crossfade"
                        ) { activeTab ->
                            when (activeTab) {
                                DualDockTab.VAULT -> VaultScreen(
                                    state = state,
                                    getCachedIconBitmap = viewModel::getCachedIconBitmap,
                                    loadIconBitmap = viewModel::loadIconBitmap,
                                    onSelectFilter = viewModel::selectVaultFilter,
                                    onLaunchSpace = { viewModel.requestLaunchSpace(it) },
                                    onDuplicateSpace = viewModel::duplicateClone,
                                    onMoreSpace = viewModel::openCloneActions,
                                    onQuickPresetClick = { viewModel.openPresetCloneConfig(it) },
                                    onOpenAppPicker = { viewModel.selectTab(DualDockTab.APPS) },
                                    onLockClick = viewModel::lockVaultNow,
                                    modifier = contentModifier
                                )

                                DualDockTab.APPS -> AppsPickerScreen(
                                    state = state,
                                    getCachedIconBitmap = viewModel::getCachedIconBitmap,
                                    loadIconBitmap = viewModel::loadIconBitmap,
                                    onSearchChange = viewModel::updateAppSearchQuery,
                                    onSelectCategory = viewModel::selectPresetCategory,
                                    onPresetClick = { viewModel.openPresetCloneConfig(it) },
                                    onInstalledAppClick = viewModel::openInstalledAppCloneConfig,
                                    onCustomSpaceClick = viewModel::openCustomContainerConfig,
                                    modifier = contentModifier
                                )

                                DualDockTab.WORK_SPACE -> WorkSpaceScreen(
                                    state = state,
                                    getCachedIconBitmap = viewModel::getCachedIconBitmap,
                                    loadIconBitmap = viewModel::loadIconBitmap,
                                    onSetupWorkProfile = viewModel::requestWorkProfileSetup,
                                    onRefreshStatus = { viewModel.refreshDeviceState(force = true) },
                                    onOpenWorkPlayStore = viewModel::openWorkPlayStore,
                                    onOpenSettings = viewModel::openDeviceAccountSettings,
                                    onLaunchWorkApp = viewModel::launchWorkProfileAppDirect,
                                    onImportWorkApp = viewModel::openWorkAppImportConfig,
                                    onJumpToContainers = { viewModel.selectTab(DualDockTab.APPS) },
                                    onQuickNativeClone = viewModel::openNativeAppViaWorkSpace,
                                    modifier = contentModifier
                                )

                                DualDockTab.SECURITY -> SecurityScreen(
                                    state = state,
                                    getCachedIconBitmap = viewModel::getCachedIconBitmap,
                                    loadIconBitmap = viewModel::loadIconBitmap,
                                    onSetupOrChangePin = viewModel::openPinSetupSheet,
                                    onRemovePin = viewModel::removeVaultPin,
                                    onToggleVaultLock = viewModel::toggleVaultLockMaster,
                                    onToggleBiometric = viewModel::toggleBiometricUnlock,
                                    onSelectAutoLockTimeout = viewModel::setAutoLockTimeout,
                                    onLockNow = viewModel::lockVaultNow,
                                    onToggleSpaceLock = viewModel::toggleCloneLock,
                                    modifier = contentModifier
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheets
        state.activeConfigDraft?.let { draft ->
            val iconBitmap = rememberAsyncAppIcon(
                packageName = draft.packageName,
                getCachedIcon = viewModel::getCachedIconBitmap,
                loadIcon = viewModel::loadIconBitmap
            )
            CloneConfigBottomSheet(
                draft = draft,
                cachedIconBitmap = iconBitmap,
                canPinShortcut = state.isShortcutPinSupported,
                onDismiss = viewModel::dismissConfigDraft,
                onSave = viewModel::saveCloneDraft,
                onUseNativeWorkMode = viewModel::openNativeAppViaWorkSpace
            )
        }

        state.selectedActionsSpace?.let { space ->
            val iconBitmap = rememberAsyncAppIcon(
                packageName = space.packageName,
                getCachedIcon = viewModel::getCachedIconBitmap,
                loadIcon = viewModel::loadIconBitmap
            )
            CloneActionsBottomSheet(
                space = space,
                cachedIconBitmap = iconBitmap,
                onDismiss = viewModel::dismissCloneActions,
                onLaunch = { viewModel.requestLaunchSpace(space) },
                onDuplicate = { viewModel.duplicateClone(space) },
                onPinShortcut = { viewModel.pinShortcutForSpace(space) },
                onEdit = { viewModel.openEditCloneConfig(space) },
                onToggleLock = { viewModel.toggleCloneLock(space) },
                onClearSession = { viewModel.requestLaunchSpace(space, clearSession = true) },
                onDelete = { viewModel.deleteClone(space) }
            )
        }

        if (state.showPinSetupSheet) {
            PinSetupBottomSheet(
                onDismiss = viewModel::dismissPinSetupSheet,
                onPinConfirmed = viewModel::saveNewPin
            )
        }

        // Full-Screen Vault / Per-Clone Privacy Lock Overlay
        AnimatedVisibility(
            visible = isLockedOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val pendingSpace = state.pendingUnlockSpace
            val canUseBio = state.securitySettings.isBiometricEnabled &&
                state.biometricAvailability == BiometricAvailability.AVAILABLE

            LaunchedEffect(isLockedOverlayVisible) {
                if (isLockedOverlayVisible && canUseBio && pendingSpace == null) {
                    viewModel.triggerBiometricUnlock()
                }
            }

            VaultLockScreen(
                title = pendingSpace?.displayName ?: "DualDock Vault",
                subtitle = if (pendingSpace != null) {
                    "Enter Vault PIN to open ${pendingSpace.slotLabel}"
                } else {
                    "Enter your Vault PIN to unlock DualDock"
                },
                pinLength = state.securitySettings.pinLength,
                canUseBiometric = canUseBio,
                errorText = state.lockErrorMessage,
                onVerifyPin = viewModel::verifyUnlockPin,
                onBiometricClick = viewModel::triggerBiometricUnlock,
                onCancel = if (pendingSpace != null && !state.isVaultLocked) {
                    viewModel::cancelPendingSpaceUnlock
                } else {
                    null
                }
            )
        }

        // Pure-White Launch Splash Intro showing ONLY the DualDock logo & app name with smooth GPU animation
        AnimatedVisibility(
            visible = !state.hasCompletedLaunchIntro,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 1.04f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        ) {
            DualDockLaunchSplash(
                onFinished = viewModel::onLaunchIntroFinished
            )
        }
    }
}

@Composable
private fun DualDockLaunchSplash(
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    val initialOffsetPx = remember(density) { with(density) { 14.dp.toPx() } }
    val smoothCurve = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }

    val logoScale = remember { Animatable(0.86f) }
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val nameOffsetY = remember { Animatable(initialOffsetPx) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = smoothCurve)
                )
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                delay(90L)
                nameAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 340, easing = smoothCurve)
                )
            }
            launch {
                delay(90L)
                nameOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 360, easing = smoothCurve)
                )
            }
        }
        delay(480L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("launch_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF18162E),
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.14f),
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dualdock_icon_v2_1795722351273),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = JetInk,
                modifier = Modifier.graphicsLayer {
                    alpha = nameAlpha.value
                    translationY = nameOffsetY.value
                }
            )
        }
    }
}

/**
 * Iconic Jet-Black Pill Floating Navigation Bar with Pure-White circular active puck,
 * closely modeled after the user's attached UI reference.
 */
@Composable
private fun JetPillFloatingNavBar(
    selectedTab: DualDockTab,
    onSelectTab: (DualDockTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = JetInk,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(68.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = JetInk.copy(alpha = 0.25f),
                    spotColor = JetInk.copy(alpha = 0.25f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dualDockDestinations.forEach { dest ->
                    val selected = selectedTab == dest.tab
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (selected) PureWhite else Color.Transparent)
                            .clickable { onSelectTab(dest.tab) }
                            .testTag("nav_${dest.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.tab.label,
                            tint = if (selected) JetInk else PureWhite.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
