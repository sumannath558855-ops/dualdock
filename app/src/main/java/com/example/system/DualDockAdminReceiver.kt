package com.example.system

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.MainActivity

class DualDockAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return
        val adminComponent = getComponentName(context)
        try {
            dpm.setProfileName(adminComponent, "DualDock Work")
            dpm.setProfileEnabled(adminComponent)
        } catch (_: SecurityException) {
            // Device policy restriction on certain OEM builds.
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
    }

    companion object {
        fun getComponentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, DualDockAdminReceiver::class.java)
    }
}
