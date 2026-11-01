package net.ddns.mlsoftlaberge.trycorder.trycorder

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.InfoSection
import net.ddns.mlsoftlaberge.trycorder.core.report
import net.ddns.mlsoftlaberge.trycorder.media.CaptureResult

/** The bottom viewer area and the button groups (switchviewer / switchbuttonlayout). */
internal class ViewerActions(private val ctx: TrycorderContext, private val sensors: SensorActions) {
    private var statsJob: Job? = null
    private var pictureId = 0

    /** switchbuttonlayout() */
    fun showGroup(group: ButtonGroup) {
        GROUP_MESSAGES[group]?.let { ctx.say(it) }
        ctx.update { it.copy(buttonGroup = group) }
    }

    /** switchviewer() */
    fun show(page: ViewerPage) {
        statsJob?.cancel()
        statsJob = null
        ctx.say(PAGE_MESSAGES.getValue(page))
        val info = ctx.services.systemInfo
        val infoText = when (page) {
            ViewerPage.LogsInfo -> info.report(
                InfoSection.Connectivity, InfoSection.Telephony, InfoSection.Network, InfoSection.System,
                InfoSection.OperSys, InfoSection.Package, InfoSection.WifiDhcp,
            )
            ViewerPage.SystemInfo -> info.report(
                InfoSection.Connectivity, InfoSection.Sensors, InfoSection.Cpu, InfoSection.Memory,
            )
            else -> ctx.state.infoText
        }
        if (page == ViewerPage.LogsStat) statsJob = sensors.startMemoryStats()
        ctx.update { it.copy(viewerPage = page, infoText = infoText) }
    }

    fun mainViewer() {
        ctx.update { it.copy(viewerFront = false) }
        show(ViewerPage.Camera)
    }

    fun localViewer() {
        ctx.update { it.copy(viewerFront = true) }
        show(ViewerPage.Camera)
    }

    fun nextPlan() = ctx.update {
        it.copy(planIndex = (it.planIndex + 1) % TrycorderState.PLAN_COUNT)
    }

    /** snapphoto(): take a picture from the live viewer, then show it with the faces found. */
    fun snap() {
        val camera = ctx.services.camera
        if (ctx.state.viewerPage != ViewerPage.Camera || !camera.isActive) {
            ctx.buttonBad()
            return
        }
        ctx.buttonSound()
        ctx.say("Picture taken")
        ctx.scope.launch {
            val result = camera.snap()
            if (result == null) {
                ctx.say("Something bad happened with camera")
                return@launch
            }
            ctx.say("Picture taken and saved!")
            show(ViewerPage.Photo)
            ctx.say("Faces Found ${result.faces.size}")
            ctx.update { it.copy(picture = ViewerPicture(result.jpeg, result.faces, ++pictureId)) }
        }
    }

    /** takephoto(): open the system camera app and show the result. */
    fun takePhoto() {
        ctx.say("Open Photo application")
        show(ViewerPage.Photo)
        ctx.scope.launch {
            when (val result = ctx.services.mediaCapture.takePhoto()) {
                is CaptureResult.Saved -> {
                    ctx.say("Saved to ${result.uri}")
                    show(ViewerPage.Photo)
                    val bytes = ctx.services.mediaLibrary.read(result.uri)
                    if (bytes != null) ctx.update { it.copy(picture = ViewerPicture(bytes, emptyList(), ++pictureId)) }
                }
                CaptureResult.Cancelled -> ctx.say("Cancelled Photo")
                CaptureResult.Failed -> ctx.say("Failed Saving Photo")
            }
        }
    }

    /** recordvideo(): open the system camera app in video mode. */
    fun recordVideo() {
        ctx.say("Open Video application")
        ctx.scope.launch {
            when (val result = ctx.services.mediaCapture.recordVideo()) {
                is CaptureResult.Saved -> ctx.say("Saved to ${result.uri}")
                CaptureResult.Cancelled -> ctx.say("Cancelled Video")
                CaptureResult.Failed -> ctx.say("Failed Saving Video")
            }
        }
    }

    fun stopStats() {
        statsJob?.cancel()
        statsJob = null
    }

    private companion object {
        val GROUP_MESSAGES = mapOf(
            ButtonGroup.Sensor to "Sensors Mode",
            ButtonGroup.Comm to "Communication Mode",
            ButtonGroup.Shield to "Shield Mode",
            ButtonGroup.Fire to "Fire Mode",
            ButtonGroup.Transporter to "Transporter Mode",
            ButtonGroup.Tractor to "Tractor Mode",
            ButtonGroup.Motor to "Motor Mode",
            ButtonGroup.Viewer to "Viewer Mode",
            ButtonGroup.Logs to "Logs Mode",
            ButtonGroup.Mode to "Mode Mode",
        )

        val PAGE_MESSAGES = mapOf(
            ViewerPage.FederationLogo to "Viewer OFF",
            ViewerPage.Camera to "Viewer ON",
            ViewerPage.LogsConsole to "Logs Console",
            ViewerPage.LogsInfo to "Logs Info",
            ViewerPage.Plans to "Plans",
            ViewerPage.Photo to "View Snap Photo",
            ViewerPage.SystemInfo to "System Info",
            ViewerPage.Animate to "Animate Viewer",
            ViewerPage.SpeakList to "Speak List",
            ViewerPage.LogsStat to "Logs Stat",
            ViewerPage.ModeWindow to "Mode Window",
            ViewerPage.TransporterSeek to "Transporter Seek Window",
            ViewerPage.FireControl to "Fire Control Window",
            ViewerPage.TractorBeam to "Tractor Beam Window",
            ViewerPage.CommandControl to "Command Control Window",
        )
    }
}
