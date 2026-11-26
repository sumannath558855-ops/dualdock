package com.example.container

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import com.example.data.ClonedSpace
import com.example.data.ClonedSpaceRepository
import com.example.data.DualDockDatabase
import com.example.data.SecurityPreferences
import com.example.data.SecuritySettings
import com.example.system.BiometricAuthHelper
import com.example.system.BiometricAvailability
import com.example.system.CloneSlotLauncher
import com.example.system.ShortcutHelper
import com.example.system.WorkProfileManager
import com.example.ui.screens.VaultLockScreen
import com.example.ui.theme.DualDockTheme
import kotlinx.coroutines.launch

class ShortcutLaunchActivity : FragmentActivity() {

    private lateinit var repository: ClonedSpaceRepository
    private lateinit var securityPreferences: SecurityPreferences
    private lateinit var workProfileManager: WorkProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        repository = ClonedSpaceRepository(DualDockDatabase.getInstance(this).clonedSpaceDao())
        securityPreferences = SecurityPreferences(this)
        workProfileManager = WorkProfileManager(this)

        val cloneId = intent.getLongExtra(ShortcutHelper.EXTRA_CLONE_ID, -1L)
        if (cloneId <= 0L) {
            openMainAppAndFinish()
            return
        }

        lifecycleScope.launch {
            try {
                val space = repository.getById(cloneId)
                val security = securityPreferences.getCurrentSettings()
                if (space == null) {
                    openMainAppAndFinish()
                    return@launch
                }

                val requiresAuth = security.hasPin && (security.isVaultLockEnabled || space.isLocked)
                if (!requiresAuth) {
                    launchSpaceAndFinish(space)
                } else {
                    showLockGate(space, security)
                }
            } catch (_: Exception) {
                openMainAppAndFinish()
            }
        }
    }

    private fun showLockGate(space: ClonedSpace, security: SecuritySettings) {
        val biometricAvailable =
            security.isBiometricEnabled &&
                BiometricAuthHelper.checkAvailability(this) == BiometricAvailability.AVAILABLE

        setContent {
            DualDockTheme {
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                val triggerBiometric = {
                    BiometricAuthHelper.authenticate(
                        activity = this@ShortcutLaunchActivity,
                        title = "Unlock ${space.displayName}",
                        subtitle = "Verify your identity to open this cloned space",
                        onSuccess = {
                            scope.launch { launchSpaceAndFinish(space) }
                        },
                        onError = { msg -> errorMessage = msg }
                    )
                }

                LaunchedEffect(Unit) {
                    if (biometricAvailable) {
                        triggerBiometric()
                    }
                }

                VaultLockScreen(
                    title = space.displayName,
                    subtitle = "Enter your DualDock PIN to launch ${space.slotLabel}",
                    pinLength = security.pinLength,
                    canUseBiometric = biometricAvailable,
                    errorText = errorMessage,
                    onVerifyPin = { candidate ->
                        scope.launch {
                            if (securityPreferences.verifyPin(candidate)) {
                                launchSpaceAndFinish(space)
                            } else {
                                errorMessage = getString(R.string.pin_error_wrong)
                            }
                        }
                    },
                    onBiometricClick = triggerBiometric,
                    onCancel = { finish() }
                )
            }
        }
    }

    private suspend fun launchSpaceAndFinish(space: ClonedSpace) {
        repository.recordLaunch(space.id)
        val launched = CloneSlotLauncher.launchSpace(this, space, workProfileManager)
        if (!launched) {
            openMainAppAndFinish()
        } else {
            finish()
        }
    }

    private fun openMainAppAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        finish()
    }
}
