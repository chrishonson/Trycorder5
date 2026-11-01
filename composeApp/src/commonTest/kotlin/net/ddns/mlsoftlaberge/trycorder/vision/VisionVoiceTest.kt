package net.ddns.mlsoftlaberge.trycorder.vision

import net.ddns.mlsoftlaberge.trycorder.camera.CameraEffect
import net.ddns.mlsoftlaberge.trycorder.camera.CameraScene
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisionVoiceTest {
    @Test
    fun languageSwitches() {
        assertEquals(
            VoiceReply(TrekLanguage.French, "français", TrekLanguage.French),
            VisionVoice.reply("Parle Français", TrekLanguage.English),
        )
        assertEquals(
            VoiceReply(TrekLanguage.English, "english", TrekLanguage.English),
            VisionVoice.reply("anglais", TrekLanguage.French),
        )
    }

    @Test
    fun answersFollowTheLanguage() {
        assertEquals("State your question", VisionVoice.reply("Computer", TrekLanguage.English)?.speech)
        assertEquals("Faites votre requète", VisionVoice.reply("ordinateur", TrekLanguage.French)?.speech)
        assertEquals("This is not very polite.", VisionVoice.reply("oh shit", TrekLanguage.English)?.speech)
        assertEquals("Martin is my Master.", VisionVoice.reply("who is your master", TrekLanguage.Auto)?.speech)
        assertNull(VisionVoice.reply("hello", TrekLanguage.English))
    }

    @Test
    fun modeTablesKeepTheOriginalOrder() {
        assertEquals(9, VisionModes.NIGHT_EFFECTS.size)
        assertEquals(CameraEffect.Mono, VisionModes.NIGHT_EFFECTS.first())
        assertEquals(CameraEffect.None, VisionModes.NIGHT_EFFECTS.last())
        assertEquals(CameraEffect.None, VisionModes.DAY_EFFECTS.first())
        assertEquals(CameraEffect.Aqua, VisionModes.DAY_EFFECTS.last())
        val text = VisionModes.describe(CameraEffect.Blackboard, CameraScene.NightPortrait)
        assertEquals("View Blackboard, Night Portrait", text)
    }
}
