package net.ddns.mlsoftlaberge.trycorder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ddns.mlsoftlaberge.trycorder.camera.CameraController
import net.ddns.mlsoftlaberge.trycorder.camera.SnapResult
import net.ddns.mlsoftlaberge.trycorder.trycorder.ButtonGroup
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrycorderIntent
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrycorderLayout
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrycorderState
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrycorderLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val camera = object : CameraController {
        override val isActive = false
        override suspend fun snap(): SnapResult? = null
    }

    @Test
    fun showsTheLcarsBarsAndColumn() {
        compose.setContent { TrycorderLayout(TrycorderState(), camera) {} }
        listOf("Sound", "Start", "Stop", "Setup", "Snap", "Photo", "Record", "Gallery", "Ready").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun leftColumnSelectsAGroup() {
        val intents = mutableListOf<TrycorderIntent>()
        compose.setContent { TrycorderLayout(TrycorderState(), camera) { intents += it } }
        compose.onNodeWithText("MODE").performClick()
        assertEquals(listOf(TrycorderIntent.SelectGroup(ButtonGroup.Mode)), intents)
    }

    @Test
    fun groupRowDispatchesItsCommand() {
        val intents = mutableListOf<TrycorderIntent>()
        compose.setContent {
            TrycorderLayout(TrycorderState(buttonGroup = ButtonGroup.Fire), camera) { intents += it }
        }
        compose.onNodeWithText("PHASER").performClick()
        assertEquals(listOf(TrycorderIntent.Command(TrekCommand.Phaser)), intents)
    }
}
