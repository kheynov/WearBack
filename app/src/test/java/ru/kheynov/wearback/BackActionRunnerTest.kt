package ru.kheynov.wearback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackActionRunnerTest {
    private var time = 0L
    private var ready = true
    private var succeeds = true
    private var calls = 0
    private var failures = 0
    private val scheduled = mutableListOf<Pair<Long, () -> Unit>>()
    private val runner = BackActionRunner(
        now = { time },
        schedule = { delay, action -> scheduled += delay to action },
        deviceReady = { ready },
        performBack = { calls++; succeeds },
        onFailure = { failures++ },
    )

    private fun runPending() {
        val tasks = scheduled.toList()
        scheduled.clear()
        tasks.forEach { it.second() }
    }

    @Test fun waitsForTheNoDisplayActivityToFinish() {
        runner.request()
        assertEquals(0, calls)
        assertEquals(BackActionRunner.SETTLE_DELAY_MS, scheduled.single().first)
        time = BackActionRunner.SETTLE_DELAY_MS
        runPending()
        assertEquals(1, calls)
    }

    @Test fun eachPressPerformsExactlyOneBack() {
        repeat(3) { runner.request() }
        runPending()
        assertEquals(3, calls)
        assertEquals(0, failures)
    }

    @Test fun aDeliveredCallbackCannotPerformBackTwice() {
        runner.request()
        val action = scheduled.single().second
        repeat(2) { action() }
        assertEquals(1, calls)
    }

    @Test fun lockedOrSleepingDeviceDoesNotScheduleBack() {
        ready = false
        runner.request()
        assertTrue(scheduled.isEmpty())
        assertEquals(0, calls)
    }

    @Test fun lockingOrSleepingDuringTheDelayCancelsBack() {
        runner.request()
        ready = false
        runPending()
        assertEquals(0, calls)
    }

    @Test fun disconnectCancelsPendingBack() {
        runner.request()
        runner.close()
        runPending()
        assertEquals(0, calls)
    }

    @Test fun closedRunnerRejectsNewRequests() {
        runner.close()
        runner.request()
        assertTrue(scheduled.isEmpty())
    }

    @Test fun staleRequestsAreNotPerformedAfterAStall() {
        runner.request()
        time = BackActionRunner.REQUEST_LIFETIME_MS + 1
        runPending()
        assertEquals(0, calls)
    }

    @Test fun failedGlobalActionIsReportedWithoutRetry() {
        succeeds = false
        runner.request()
        runPending()
        assertEquals(1, calls)
        assertEquals(1, failures)
        assertTrue(scheduled.isEmpty())
    }
}
