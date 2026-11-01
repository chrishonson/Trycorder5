package net.ddns.mlsoftlaberge.trycorder.multisensor

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MultiSensorVoiceTest {
    @Test
    fun languageSentencesSwitchLanguage() {
        assertEquals(
            VoiceReply("français", TrekLanguage.French, TrekLanguage.French),
            MultiSensorVoice.reply("Parle Français", TrekLanguage.English),
        )
        assertEquals(
            VoiceReply("english", TrekLanguage.English, TrekLanguage.English),
            MultiSensorVoice.reply("anglais", TrekLanguage.French),
        )
    }

    @Test
    fun answersFollowTheSpeakLanguage() {
        assertEquals("Martin is my Master.", MultiSensorVoice.reply("who is your master", TrekLanguage.English)?.speech)
        assertEquals("Martin est mon maître.", MultiSensorVoice.reply("Martin", TrekLanguage.French)?.speech)
        assertEquals("State your question", MultiSensorVoice.reply("Computer", TrekLanguage.English)?.speech)
        assertEquals("Ce n'est pas très poli", MultiSensorVoice.reply("shit", TrekLanguage.French)?.speech)
    }

    @Test
    fun otherSentencesHaveNoAnswer() {
        assertNull(MultiSensorVoice.reply("raise shields", TrekLanguage.English))
    }
}
