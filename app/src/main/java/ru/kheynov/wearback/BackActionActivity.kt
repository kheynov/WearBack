package ru.kheynov.wearback

import android.app.Activity
import android.os.Bundle

class BackActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (savedInstanceState == null && isReadyForBack()) {
                BackAccessibilityService.requestBack()
            }
        } finally {
            // Theme.NoDisplay requires finishing before onResume, even when the service is off.
            finish()
        }
    }
}
