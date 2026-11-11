package ru.kheynov.wearback

internal class BackActionRunner(
    private val now: () -> Long,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val deviceReady: () -> Boolean,
    private val performBack: () -> Boolean,
    private val onFailure: () -> Unit,
) {
    private var active = true

    fun request() {
        if (!active || !deviceReady()) return
        val requestedAt = now()
        var consumed = false
        // The no-display activity finishes before this runs. It never owns an input window.
        schedule(SETTLE_DELAY_MS) {
            if (!consumed) {
                consumed = true
                if (active && now() - requestedAt <= REQUEST_LIFETIME_MS && deviceReady()) {
                    if (!performBack()) onFailure()
                }
            }
        }
    }

    fun close() {
        active = false
    }

    companion object {
        const val SETTLE_DELAY_MS = 150L
        const val REQUEST_LIFETIME_MS = 1_000L
    }
}
