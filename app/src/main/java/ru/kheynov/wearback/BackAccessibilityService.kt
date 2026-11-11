package ru.kheynov.wearback

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class BackAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var runner: BackActionRunner? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        disconnect()
        runner = BackActionRunner(
            now = SystemClock::uptimeMillis,
            schedule = { delay, action -> handler.postDelayed({ action() }, delay) },
            deviceReady = { isReadyForBack() },
            performBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            onFailure = {
                Toast.makeText(this, R.string.back_failed, Toast.LENGTH_SHORT).show()
            },
        )
        connected = this
    }

    // No event types are requested, and no screen content or window tree is accessed.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        // Cancel pending actions, but keep the connected service available for the next press.
        handler.removeCallbacksAndMessages(null)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    private fun disconnect() {
        if (connected === this) connected = null
        runner?.close()
        runner = null
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        @Volatile
        private var connected: BackAccessibilityService? = null

        internal fun requestBack() {
            // No UI or queued request when the user has not enabled the service.
            connected?.runner?.request()
        }
    }
}
