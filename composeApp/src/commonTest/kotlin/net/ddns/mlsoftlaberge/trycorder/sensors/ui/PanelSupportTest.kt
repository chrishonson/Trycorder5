package net.ddns.mlsoftlaberge.trycorder.sensors.ui

import net.ddns.mlsoftlaberge.trycorder.sensors.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelSupportTest {
    @Test
    fun historyDropsTheOldestValueWhenFull() {
        val history = TraceHistory(3)
        (1..5).forEach { history.add(Vector3(it.toFloat())) }
        assertEquals(3, history.size)
        assertEquals(listOf(3f, 4f, 5f), history.toList().map { it.x })
        assertEquals(3f, history[0].x)
    }

    @Test
    fun fixedFormatMatchesStringFormat() {
        assertEquals("12.35", FixedFormat.format(12.345f, 2))
        assertEquals("0.00", FixedFormat.format(0f, 2))
        assertEquals("-3.50", FixedFormat.format(-3.5f, 2))
        assertEquals("1200", FixedFormat.format(1200f, 0))
        assertEquals("-20", FixedFormat.format(-20f, 0))
        assertEquals("0.07", FixedFormat.format(0.07f, 2))
        assertEquals("0", FixedFormat.format(-0.001f, 0))
    }

    @Test
    fun fadeAlphaFollowsTheShieldAndTransporterRules() {
        assertEquals(255, PanelAnimation.fadeAlpha(0, raising = true))
        assertEquals(0, PanelAnimation.fadeAlpha(0, raising = false))
        assertEquals(100, PanelAnimation.fadeAlpha(100, raising = true))
        assertEquals(155, PanelAnimation.fadeAlpha(100, raising = false))
        assertEquals(255, PanelAnimation.fadeAlpha(300, raising = true))
    }

    @Test
    fun beamFrequencyWrapsAfterTen() {
        assertEquals(1, PanelAnimation.nextFreq(0))
        assertEquals(10, PanelAnimation.nextFreq(9))
        assertEquals(0, PanelAnimation.nextFreq(10))
    }
}
