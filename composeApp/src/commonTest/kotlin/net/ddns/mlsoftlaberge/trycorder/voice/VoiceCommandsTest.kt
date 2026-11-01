package net.ddns.mlsoftlaberge.trycorder.voice

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceCommandsTest {
    private fun command(text: String) = (VoiceCommands.parse(text) as? VoiceMatch.Command)?.command

    @Test
    fun sentencesWithoutPrefixAreNotCommands() {
        assertNull(VoiceCommands.parse("phaser"))
        assertNull(VoiceCommands.parse("hello computer phaser"))
    }

    @Test
    fun prefixesInBothLanguages() {
        assertEquals(TrekCommand.Phaser, command("Computer phaser"))
        assertEquals(TrekCommand.Phaser, command("ordinateur phaser"))
    }

    @Test
    fun firstMatchWinsInTheOriginalOrder() {
        assertEquals(TrekCommand.SensorOff, command("computer sensor off"))
        assertEquals(TrekCommand.Magnetic, command("computer sensor"))
        assertEquals(TrekCommand.HailingClose, command("computer hailing close"))
        assertEquals(TrekCommand.Hailing, command("computer hailing frequency"))
        assertEquals(TrekCommand.LowerShield, command("computer lower shield"))
        assertEquals(TrekCommand.RaiseShield, command("computer shield"))
        // "fire" matches before the beam commands, like the original
        assertEquals(TrekCommand.Torpedo, command("computer fire torpedo"))
        assertEquals(TrekCommand.BeamMeUp, command("computer beam me up scotty"))
        // "viewer on" is tested before "local viewer"
        assertEquals(TrekCommand.LocalViewer, command("computer local viewer"))
    }

    @Test
    fun languageAndRudenessDoNotNeedThePrefix() {
        assertEquals(VoiceMatch.SwitchLanguage(TrekLanguage.French), VoiceCommands.parse("français"))
        assertEquals(VoiceMatch.SwitchLanguage(TrekLanguage.English), VoiceCommands.parse("anglais"))
        assertEquals(VoiceMatch.Rude, VoiceCommands.parse("oh shit"))
    }

    @Test
    fun unknownCommandAfterPrefixIsNull() {
        assertNull(VoiceCommands.parse("computer make coffee"))
    }

    @Test
    fun pickCandidatePrefersCommands() {
        assertEquals("hello", VoiceCommands.pickCandidate(listOf("hello", "yellow")))
        assertEquals("computer fire", VoiceCommands.pickCandidate(listOf("compute fire", "computer fire")))
        assertNull(VoiceCommands.pickCandidate(emptyList()))
    }

    @Test
    fun looksLikeCommandMatchesTheServiceFilter() {
        assertTrue(VoiceCommands.looksLikeCommand("server ok"))
        assertTrue(VoiceCommands.looksLikeCommand("computer anything"))
        assertFalse(VoiceCommands.looksLikeCommand("red alert"))
    }
}
