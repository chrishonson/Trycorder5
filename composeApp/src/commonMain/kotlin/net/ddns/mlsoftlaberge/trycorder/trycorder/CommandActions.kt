package net.ddns.mlsoftlaberge.trycorder.trycorder

import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand

/**
 * The starship commands, triggered by the group buttons or by voice / network ("computer ...").
 * Each entry keeps the group, the text echoed to the other Trycorders and the action of the original.
 */
internal class CommandActions(
    private val ctx: TrycorderContext,
    private val sensors: SensorActions,
    private val viewer: ViewerActions,
) {
    private class Spec(
        val group: ButtonGroup?,
        val echo: String?,
        val clickSound: Boolean,
        val action: () -> Unit,
    )

    /**
     * Run [command]. From a button: optional click sound, the action, then the echo to the
     * other Trycorders. From voice or network: switch to the command's button group first, no echo.
     */
    fun execute(command: TrekCommand, fromVoice: Boolean) {
        val spec = specs.getValue(command)
        if (fromVoice) {
            spec.group?.let { viewer.showGroup(it) }
            spec.action()
        } else {
            if (spec.clickSound) ctx.buttonSound()
            spec.action()
            spec.echo?.let { ctx.sendCommand(it) }
        }
    }

    private val specs: Map<TrekCommand, Spec> = mapOf(
        TrekCommand.SensorOff to Spec(ButtonGroup.Sensor, "sensor off", true) {
            ctx.speakIfChatty("Sensors off")
            sensors.showPage(SensorPage.EarthStill)
            sensors.stop()
        },
        TrekCommand.Magnetic to scan(SensorPage.Magnetic, "magnetic", "Magnetic sensor"),
        TrekCommand.Orientation to scan(SensorPage.Orientation, "orientation", "Orientation sensor"),
        TrekCommand.Gravity to scan(SensorPage.Gravity, "gravity", "Gravity sensor"),
        TrekCommand.Temperature to scan(SensorPage.Temperature, "temperature", "Temperature sensor"),
        TrekCommand.Hailing to Spec(ButtonGroup.Comm, "hailing", false) { comm(CommStatus.Hailing) },
        TrekCommand.HailingClose to Spec(ButtonGroup.Comm, "hailing close", false) { comm(CommStatus.Closed) },
        TrekCommand.Intercom to Spec(ButtonGroup.Comm, "intercom", false) { comm(CommStatus.Intercom) },
        TrekCommand.ChatComm to Spec(ButtonGroup.Comm, "chatcomm", false) { comm(CommStatus.Chat) },
        TrekCommand.RaiseShield to Spec(ButtonGroup.Shield, "raise shield", false) {
            animate(TrekSound.ShieldUp, "Raise Shields", SensorPage.Shields, "Shields Up") { it.copy(shieldMode = 1) }
        },
        TrekCommand.LowerShield to Spec(ButtonGroup.Shield, "lower shield", false) {
            animate(TrekSound.ShieldDown, "Lower Shields", SensorPage.Shields, "Shields Down") { it.copy(shieldMode = 2) }
        },
        TrekCommand.Phaser to Spec(ButtonGroup.Fire, "phaser", false) {
            animate(TrekSound.Phaser, "Fire Phaser", SensorPage.Fire, "The target is disabled") { it.copy(fireMode = 2) }
        },
        TrekCommand.Torpedo to Spec(ButtonGroup.Fire, "fire", false) {
            animate(TrekSound.Torpedo, "Fire Torpedo", SensorPage.Fire, "The ship is destroyed") { it.copy(fireMode = 1) }
        },
        TrekCommand.BeamMeUp to Spec(ButtonGroup.Transporter, "beam me up", false) {
            ctx.speakIfChatty("Transport In Progress.")
            animate(TrekSound.BeamIn, "Transport In", SensorPage.Transporter, "Transport Complete") {
                it.copy(transporterMode = 1)
            }
        },
        TrekCommand.BeamMeDown to Spec(ButtonGroup.Transporter, "beam me down", false) {
            ctx.speakIfChatty("Transport In Progress.")
            animate(TrekSound.BeamOut, "Transport Out", SensorPage.Transporter, "Transport Complete") {
                it.copy(transporterMode = 2)
            }
        },
        TrekCommand.TractorPush to Spec(ButtonGroup.Tractor, "tractor push", false) {
            animate(TrekSound.Tractor, "Engage Tractor Beam Push", SensorPage.Tractor, "Repulser Beam Engaged") {
                it.copy(tractorMode = 1)
            }
        },
        TrekCommand.TractorOff to Spec(ButtonGroup.Tractor, "tractor off", false) {
            halt(TrekSound.KeyOk, "Tractor Beam Off", SensorPage.Tractor, "Beam Off") { it.copy(tractorMode = 0) }
        },
        TrekCommand.TractorPull to Spec(ButtonGroup.Tractor, "tractor pull", false) {
            animate(TrekSound.Tractor, "Engage Tractor Beam Pull", SensorPage.Tractor, "Tractor Beam Engaged") {
                it.copy(tractorMode = 2)
            }
        },
        TrekCommand.ImpulsePower to Spec(ButtonGroup.Motor, "impulse power", false) {
            animate(TrekSound.Core, "Engage Impulse Engine", SensorPage.Motor, null) { it.copy(motorMode = 1) }
            motorViewer(MotorAnimation.Impulse, "Impulse Engine Engaged")
        },
        TrekCommand.StayHere to Spec(ButtonGroup.Motor, "stay here", false) {
            halt(TrekSound.SlowWarp, "Motor Off", SensorPage.Motor, null) { it.copy(motorMode = 0) }
            motorViewer(MotorAnimation.Still, "All engines down")
        },
        TrekCommand.WarpDrive to Spec(ButtonGroup.Motor, "warp drive", false) {
            animate(TrekSound.Warp, "Engage Warp Drive", SensorPage.Motor, null) { it.copy(motorMode = 2) }
            motorViewer(MotorAnimation.Warp, "Warp Drive Engaged")
        },
        TrekCommand.ViewerOn to Spec(ButtonGroup.Viewer, "main viewer", true) { viewer.mainViewer() },
        TrekCommand.LocalViewer to Spec(ButtonGroup.Viewer, "local viewer", true) { viewer.localViewer() },
        TrekCommand.ViewerOff to Spec(ButtonGroup.Viewer, "viewer off", true) { viewer.show(ViewerPage.FederationLogo) },
        TrekCommand.ViewerPhoto to Spec(ButtonGroup.Viewer, "viewer photo", true) { viewer.show(ViewerPage.Photo) },
        TrekCommand.SnapPhoto to Spec(ButtonGroup.Viewer, "snap photo", false) { viewer.snap() },
        TrekCommand.LogsConsole to page(ButtonGroup.Logs, ViewerPage.LogsConsole, "logs console"),
        TrekCommand.LogsInfo to page(ButtonGroup.Logs, ViewerPage.LogsInfo, "logs info"),
        TrekCommand.SystemPlans to page(ButtonGroup.Logs, ViewerPage.Plans, "system plans"),
        TrekCommand.SystemInfo to page(ButtonGroup.Logs, ViewerPage.SystemInfo, "system info"),
        TrekCommand.SystemStat to page(ButtonGroup.Logs, ViewerPage.LogsStat, "system stat"),
        TrekCommand.SpeakList to page(ButtonGroup.Comm, ViewerPage.SpeakList, "speak list"),
        TrekCommand.AutoDestruct to Spec(null, null, true) { ctx.play(TrekSound.DestructSequence) },
    )

    private fun scan(page: SensorPage, echo: String, chatty: String) = Spec(ButtonGroup.Sensor, echo, true) {
        ctx.speakIfChatty(chatty)
        sensors.showPage(page)
        sensors.start(page)
    }

    private fun page(group: ButtonGroup, page: ViewerPage, echo: String) = Spec(group, echo, true) { viewer.show(page) }

    /** Sound, status line, sensor page in a mode, started, optional chatty confirmation. */
    private fun animate(
        sound: TrekSound,
        message: String,
        page: SensorPage,
        chatty: String?,
        mode: (TrycorderState) -> TrycorderState,
    ) {
        ctx.play(sound)
        ctx.say(message)
        sensors.showPage(page)
        ctx.update(mode)
        sensors.start(page)
        chatty?.let { ctx.speakIfChatty(it) }
    }

    /** Same as [animate] but stops the page instead of starting it. */
    private fun halt(
        sound: TrekSound,
        message: String,
        page: SensorPage,
        chatty: String?,
        mode: (TrycorderState) -> TrycorderState,
    ) {
        ctx.play(sound)
        ctx.say(message)
        sensors.showPage(page)
        ctx.update(mode)
        sensors.stop()
        chatty?.let { ctx.speakIfChatty(it) }
    }

    private fun motorViewer(animation: MotorAnimation, chatty: String) {
        viewer.show(ViewerPage.Animate)
        ctx.update { it.copy(motorAnimation = animation) }
        ctx.speakIfChatty(chatty)
    }

    private fun comm(status: CommStatus) {
        ctx.update { it.copy(commStatus = status) }
        val (spoken, sound, page, message) = when (status) {
            CommStatus.Hailing -> CommStep("Hailing frequency opened", TrekSound.CommOpen, SensorPage.AudioWave, "Hailing frequency open.")
            CommStatus.Closed -> CommStep("Hailing frequency closed", TrekSound.CommClose, SensorPage.StartrekLogo, "Hailing frequency closed.")
            CommStatus.Intercom -> CommStep("Intercom mode ready", TrekSound.KeyOk, SensorPage.Walkie, "Intercom mode ready.")
            CommStatus.Chat -> CommStep("Chatcomm mode ready", TrekSound.ComputerBeep55, SensorPage.Chat, "Chatcomm mode ready.")
        }
        if (ctx.settings.isChatty) ctx.speak(spoken) else ctx.play(sound)
        sensors.showPage(page)
        if (status == CommStatus.Hailing) sensors.start(page) else sensors.stop()
        ctx.say(message)
    }

    private data class CommStep(val spoken: String, val sound: TrekSound, val page: SensorPage, val message: String)
}
