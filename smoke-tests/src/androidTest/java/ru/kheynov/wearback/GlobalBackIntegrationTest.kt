package ru.kheynov.wearback

import android.app.UiAutomation
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, emulator-only: temporarily enables the service and restores settings afterward. */
@RunWith(AndroidJUnit4::class)
class GlobalBackIntegrationTest {
    @Test fun disabledServiceLeavesTheUnderlyingAppUnchanged() {
        assumeTrue(
            "Enable only on a dedicated emulator",
            InstrumentationRegistry.getArguments().getString("testGlobalBack") == "true" &&
                Build.HARDWARE == "ranchu",
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        withTestService(automation, enabled = false) {
            val fixturePackage = instrumentation.context.packageName
            shell(automation, "input keyevent KEYCODE_WAKEUP")
            shell(automation, "wm dismiss-keyguard")
            shell(
                automation,
                "am start -W -f 0x10008000 -n $fixturePackage/ru.kheynov.wearback.fixture.BackStackFixtureActivity",
            )
            // With no enabled accessibility services, inspect the resumed activity via the
            // test shell instead of relying on accessibility events from the fixture.
            fun topActivityRecord(): String? = shell(automation, "dumpsys activity activities")
                .lineSequence().firstOrNull {
                    it.contains("topResumedActivity=") &&
                        it.contains("$fixturePackage/ru.kheynov.wearback.fixture.BackStackFixtureActivity")
                }?.trim()
            val originalActivity = topActivityRecord()
            assertNotNull("The independent fixture must be in the foreground", originalActivity)
            repeat(2) {
                shell(
                    automation,
                    "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                        "-n ru.kheynov.wearback/.BackActionActivity",
                )
                SystemClock.sleep(500)
                assertEquals("Disabled service must not change the foreground activity", originalActivity, topActivityRecord())
            }
        }
    }

    @Test fun launcherActionMovesBackInAnotherAppsStack() {
        assumeTrue(
            "Enable only on a dedicated emulator",
            InstrumentationRegistry.getArguments().getString("testGlobalBack") == "true" &&
                Build.HARDWARE == "ranchu",
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        withTestService(automation) {
            val fixturePackage = instrumentation.context.packageName
            shell(automation, "input keyevent KEYCODE_WAKEUP")
            shell(automation, "wm dismiss-keyguard")
            shell(
                automation,
                "am start -W -f 0x10008000 -n $fixturePackage/ru.kheynov.wearback.fixture.BackStackFixtureActivity",
            )
            waitForText(automation, "WearBack fixture depth 0")
            repeat(2) { depth ->
                val button = automation.rootInActiveWindow
                    .findAccessibilityNodeInfosByText("Next test screen").single()
                assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                waitForText(automation, "WearBack fixture depth ${depth + 1}")
            }
            repeat(2) { press ->
                shell(
                    automation,
                    "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                        "-n ru.kheynov.wearback/.BackActionActivity",
                )
                waitForText(automation, "WearBack fixture depth ${1 - press}")
                // A second unintended Back would either reveal the earlier level or close the fixture.
                SystemClock.sleep(400)
                waitForText(automation, "WearBack fixture depth ${1 - press}")
            }
        }
    }

    private fun withTestService(automation: UiAutomation, enabled: Boolean = true, test: () -> Unit) {
        val key = "enabled_accessibility_services"
        val previous = shell(automation, "settings get secure $key").trim()
        val previousEnabled = shell(automation, "settings get secure accessibility_enabled").trim()
        val component = "ru.kheynov.wearback/ru.kheynov.wearback.BackAccessibilityService"
        val others = previous.split(':').filter { it.isNotBlank() && it != "null" && it != component }
        try {
            // Starting instrumentation force-stops the target app, so rebind its service here.
            if (others.isEmpty()) shell(automation, "settings delete secure $key")
            else shell(automation, "settings put secure $key ${others.joinToString(":")}")
            SystemClock.sleep(150)
            if (enabled) {
                shell(automation, "settings put secure $key ${(others + component).joinToString(":")}")
                shell(automation, "settings put secure accessibility_enabled 1")
            }
            val deadline = SystemClock.uptimeMillis() + 5_000
            while (isServiceBound(automation) != enabled && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(50)
            }
            assertTrue("Accessibility service must reach the requested state", isServiceBound(automation) == enabled)
            test()
        } finally {
            if (previous == "null") shell(automation, "settings delete secure $key")
            else shell(automation, "settings put secure $key $previous")
            if (previousEnabled == "null") shell(automation, "settings delete secure accessibility_enabled")
            else shell(automation, "settings put secure accessibility_enabled $previousEnabled")
        }
    }

    // Black-box readiness check: R8 is free to remove/inline every private app helper.
    private fun isServiceBound(automation: UiAutomation): Boolean =
        shell(automation, "dumpsys accessibility").lineSequence().any {
            it.trimStart().startsWith("Bound services:") && it.contains("Service[label=WearBack,")
        }

    private fun shell(automation: UiAutomation, command: String): String =
        automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }

    private fun waitForText(automation: UiAutomation, text: String) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.isNotEmpty() == true) {
                return
            }
            SystemClock.sleep(50)
        }
        throw AssertionError("Expected underlying app screen: $text")
    }
}
