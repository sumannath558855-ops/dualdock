package com.example

import android.app.Application
import android.os.Build
import android.webkit.WebView

class DualDockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        configureIsolatedWebViewDirectory()
    }

    private fun configureIsolatedWebViewDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            val slotSuffix = processName.substringAfter(":", missingDelimiterValue = "")
            if (slotSuffix.startsWith("slot_")) {
                try {
                    WebView.setDataDirectorySuffix("dualdock_$slotSuffix")
                } catch (_: Throwable) {
                    // Already initialized or restricted on this OEM WebView build.
                }
            }
        }
    }
}
