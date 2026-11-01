package net.ddns.mlsoftlaberge.trycorder.link

/**
 * Delay before reconnecting to the tryserver. The original thread retried in a tight loop;
 * this doubles the wait after each failure up to [maxMillis] and starts over after a good connection.
 */
class ReconnectBackoff(private val initialMillis: Long = INITIAL_MILLIS, private val maxMillis: Long = MAX_MILLIS) {
    private var next = initialMillis

    /** The delay to wait now; the following one is twice as long. */
    fun nextDelay(): Long {
        val current = next
        next = (next * 2).coerceAtMost(maxMillis)
        return current
    }

    fun reset() {
        next = initialMillis
    }

    companion object {
        const val INITIAL_MILLIS = 1_000L
        const val MAX_MILLIS = 60_000L
    }
}
