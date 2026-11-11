package ru.kheynov.wearback

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ConfigurationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
        .createPackageContext("ru.kheynov.wearback", 0)
    // Manifest entry points are the public contract; do not retain app internals for tests.
    private val actionClassName = "ru.kheynov.wearback.BackActionActivity"
    private val serviceClassName = "ru.kheynov.wearback.BackAccessibilityService"

    @Test fun optimizedApplicationIsNotDebuggable() {
        assertFalse(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }

    @Test fun onlyTheActionLauncherEntryIsAvailable() {
        val entries = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName),
            0,
        ).map { it.activityInfo.name }.toSet()
        assertEquals(
            setOf(actionClassName),
            entries,
        )
        val activities = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_ACTIVITIES,
        ).activities.orEmpty().map { it.name }.toSet()
        assertEquals(setOf(actionClassName), activities)
    }

    @Test fun actionHasNoWindowOrTaskAffinity() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, actionClassName), 0,
        )
        val theme = context.resources.newTheme().apply { applyStyle(info.theme, true) }
        val attributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowNoDisplay))
        try {
            assertTrue(attributes.getBoolean(0, false))
        } finally {
            attributes.recycle()
        }
        assertTrue(info.taskAffinity.isNullOrEmpty())
    }

    @Test fun serviceIsProtectedAndCannotReadTheScreen() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, serviceClassName),
            PackageManager.GET_META_DATA,
        )
        assertEquals(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, info.permission)
        info.loadXmlMetaData(context.packageManager, "android.accessibilityservice").use { xml ->
            while (xml.next() != XmlPullParser.START_TAG) { /* Skip the XML header. */ }
            val android = "http://schemas.android.com/apk/res/android"
            assertFalse(xml.getAttributeBooleanValue(android, "canRetrieveWindowContent", true))
            assertFalse(xml.getAttributeBooleanValue(android, "canPerformGestures", false))
            assertEquals(null, xml.getAttributeValue(android, "accessibilityEventTypes"))
            assertEquals(null, xml.getAttributeValue(android, "settingsActivity"))
        }
    }

    @Test fun noRuntimeOrNetworkPermissionsAreRequested() {
        val info = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_PERMISSIONS,
        )
        // AGP may add its own signature-only DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION.
        val ownSignaturePermission = "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        assertTrue(info.requestedPermissions.orEmpty().all { it == ownSignaturePermission })
    }
}
