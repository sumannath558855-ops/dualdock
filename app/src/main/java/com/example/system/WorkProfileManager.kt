package com.example.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WorkProfileAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String
)

data class WorkProfileStatus(
    val isManagedUsersSupported: Boolean = false,
    val hasWorkProfile: Boolean = false,
    val isDualDockProfileOwner: Boolean = false,
    val isWorkProfilePaused: Boolean = false,
    val workApps: List<WorkProfileAppInfo> = emptyList()
)

enum class WorkNativeLaunchResult {
    LAUNCHED_NATIVE_APP,
    OPENED_WORK_PLAY_STORE,
    NEEDS_WORK_PROFILE,
    WORK_PLAY_STORE_UNAVAILABLE
}

class WorkProfileManager(private val context: Context) {

    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private fun findWorkUserHandle(): UserHandle? {
        val currentUser = Process.myUserHandle()
        val profiles = try {
            userManager?.userProfiles.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        return profiles.firstOrNull { it != currentUser }
    }

    suspend fun inspectStatus(): WorkProfileStatus = withContext(Dispatchers.IO) {
        val supported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
        val workHandle = findWorkUserHandle()
        val isOwner = try {
            dpm?.isProfileOwnerApp(context.packageName) == true
        } catch (_: Exception) {
            false
        }
        val isPaused = if (workHandle != null) {
            try {
                userManager?.isQuietModeEnabled(workHandle) == true
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }

        val apps = if (workHandle != null && launcherApps != null) {
            try {
                launcherApps.getActivityList(null, workHandle)
                    .map { info ->
                        WorkProfileAppInfo(
                            packageName = info.applicationInfo.packageName,
                            activityName = info.componentName.className,
                            label = info.label?.toString()?.trim().orEmpty()
                                .ifEmpty { info.applicationInfo.packageName }
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        WorkProfileStatus(
            isManagedUsersSupported = supported,
            hasWorkProfile = workHandle != null,
            isDualDockProfileOwner = isOwner,
            isWorkProfilePaused = isPaused,
            workApps = apps
        )
    }

    fun buildProvisioningIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                DualDockAdminReceiver.getComponentName(context)
            )
        }
    }

    fun canLaunchProvisioning(): Boolean {
        val intent = buildProvisioningIntent()
        return intent.resolveActivity(context.packageManager) != null
    }

    fun launchWorkApp(packageName: String): Boolean {
        val workHandle = findWorkUserHandle() ?: return false
        val la = launcherApps ?: return false
        return try {
            val activities = la.getActivityList(packageName, workHandle)
            val component: ComponentName = activities.firstOrNull()?.componentName ?: return false
            la.startMainActivity(component, workHandle, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWorkPlayStore(): Boolean {
        return launchWorkApp("com.android.vending")
    }

    /**
     * Launches a native app directly inside Android's Work Profile if installed,
     * otherwise opens the Work Profile Play Store so the user can install it natively
     * (giving full native features like Myntra's "Get OTP on WhatsApp").
     */
    fun openAppInWorkOrPlayStore(packageName: String): WorkNativeLaunchResult {
        val workHandle = findWorkUserHandle() ?: return WorkNativeLaunchResult.NEEDS_WORK_PROFILE
        if (packageName.isNotBlank() && launchWorkApp(packageName)) {
            return WorkNativeLaunchResult.LAUNCHED_NATIVE_APP
        }
        return if (openWorkPlayStore()) {
            WorkNativeLaunchResult.OPENED_WORK_PLAY_STORE
        } else {
            WorkNativeLaunchResult.WORK_PLAY_STORE_UNAVAILABLE
        }
    }

    fun openAccountSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
