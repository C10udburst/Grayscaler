package io.github.cloudburst.grayscaler

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

class MainService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName == null || IGNORED_PACKAGES.contains(event.packageName.toString()))
                return
            val store = AppListStore(this)
            store.load()
            if (store.shouldGrayScale(event.packageName.toString()))
                enableMonochrome()
            else
                disableMonochrome()
        }
    }

    override fun onInterrupt() {
        disableMonochrome()
    }

    private fun enableMonochrome(){
        Settings.Secure.putInt(contentResolver, DISPLAY_DALTONIZER, MONOCHROME)
        Settings.Secure.putInt(contentResolver, DISPLAY_DALTONIZER_ENABLED, ON)
    }

    private fun disableMonochrome(){
        Settings.Secure.putInt(contentResolver, DISPLAY_DALTONIZER_ENABLED, OFF)
    }

    companion object {
        const val DISPLAY_DALTONIZER = "accessibility_display_daltonizer"
        const val DISPLAY_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        const val MONOCHROME = 0
        const val OFF = 0
        const val ON = 1
        val IGNORED_PACKAGES = listOf(
            "com.android.systemui",
            "com.android.launcher3",
        )
    }
}