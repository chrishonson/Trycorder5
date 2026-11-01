package net.ddns.mlsoftlaberge.trycorder.trycorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand

/**
 * Store of the main Trycorder screen (the old TrycorderFragment).
 * [navigate] opens another screen (gallery, settings, crew...).
 */
class TrycorderStore(
    private val services: PlatformServices,
    private val navigate: (Screen) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState(services))
    val state: StateFlow<TrycorderState> = _state.asStateFlow()

    private val ctx = TrycorderContext(services, viewModelScope, _state)
    private val sensors = SensorActions(ctx)
    private val viewer = ViewerActions(ctx, sensors)
    private val commands = CommandActions(ctx, sensors, viewer)
    private val voice = VoiceActions(ctx, sensors, viewer, commands)
    private val lifecycle = LifecycleActions(ctx, sensors, viewer)

    private val barActions: Map<TrycorderIntent.Bar, () -> Unit> = mapOf(
        TrycorderIntent.Talk to voice::listen,
        TrycorderIntent.StartSensors to { clicked { sensors.start(ctx.state.sensorPage) } },
        TrycorderIntent.StopSensors to { clicked { sensors.stop() } },
        TrycorderIntent.ToggleSound to sensors::toggleSound,
        TrycorderIntent.OpenSettings to { open(Screen.Settings, "Settings", "Settings") },
        TrycorderIntent.Snap to { commands.execute(TrekCommand.SnapPhoto, fromVoice = false) },
        TrycorderIntent.TakePhoto to { clicked { viewer.takePhoto() } },
        TrycorderIntent.RecordVideo to { clicked { viewer.recordVideo() } },
        TrycorderIntent.OpenGallery to { clicked { navigate(Screen.PhotoGallery) } },
    )

    init {
        viewModelScope.launch { services.listener.results.collect { voice.understood(it) } }
        viewModelScope.launch { services.link.events.collect { voice.onLinkEvent(it) } }
    }

    fun dispatch(intent: TrycorderIntent) {
        when (intent) {
            is TrycorderIntent.Lifecycle -> onLifecycle(intent)
            is TrycorderIntent.Bar -> barActions.getValue(intent).invoke()
            is TrycorderIntent.Control -> onControl(intent)
            is TrycorderIntent.Slider -> SliderActions.dispatch(ctx, intent)
            is TrycorderIntent.Comm -> onComm(intent)
        }
    }

    private fun onLifecycle(intent: TrycorderIntent.Lifecycle) {
        when (intent) {
            TrycorderIntent.Start -> lifecycle.startServer()
            TrycorderIntent.Stop -> if (ctx.settings.autoStop) lifecycle.stopServer()
            TrycorderIntent.Resume -> lifecycle.resume()
            TrycorderIntent.Pause -> lifecycle.pause()
        }
    }

    private fun onControl(intent: TrycorderIntent.Control) {
        when (intent) {
            is TrycorderIntent.SelectGroup -> selectGroup(intent.group)
            is TrycorderIntent.Command -> commands.execute(intent.command, fromVoice = false)
            TrycorderIntent.YellowAlert -> alert(TrekSound.Alert, "Yellow ALERT !", "yellow alert")
            TrycorderIntent.RedAlert -> alert(TrekSound.RedAlert, "RED ALERT !", "red alert")
            TrycorderIntent.NextPlan -> clicked { viewer.nextPlan() }
            TrycorderIntent.FederationLogo -> ctx.buttonSound()
            TrycorderIntent.OpenMap -> openMap()
            is TrycorderIntent.OpenScreen -> openScreen(intent.screen)
        }
    }

    private fun onComm(intent: TrycorderIntent.Comm) {
        when (intent) {
            TrycorderIntent.PushToTalkDown -> ctx.say("start streaming audio").also { services.link.startAudioStream() }
            TrycorderIntent.PushToTalkUp -> ctx.say("stop streaming audio").also { services.link.stopAudioStream() }
            TrycorderIntent.CommandWindow -> clicked { viewer.show(ViewerPage.CommandControl) }
            TrycorderIntent.ServerOn -> clicked { lifecycle.startServer() }
            TrycorderIntent.ServerOff -> clicked { lifecycle.stopServer() }
            else -> onText(intent)
        }
    }

    private fun onText(intent: TrycorderIntent.Comm) {
        when (intent) {
            is TrycorderIntent.ChatInput -> ctx.update { it.copy(chatInput = intent.text) }
            TrycorderIntent.ChatSend -> chatSend(ctx.state.chatInput).also { ctx.update { it.copy(chatInput = "") } }
            is TrycorderIntent.ChatRequest -> chatSend(intent.request)
            is TrycorderIntent.CommandInput -> ctx.update { it.copy(commandInput = intent.text) }
            TrycorderIntent.CommandSend -> clicked {
                ctx.sendCommand(ctx.state.commandInput)
                ctx.update { it.copy(commandInput = "") }
            }
            else -> Unit
        }
    }

    private fun selectGroup(group: ButtonGroup) {
        ctx.buttonSound()
        viewer.showGroup(group)
        val state = ctx.state
        when (group) {
            ButtonGroup.Sensor -> sensors.showPage(state.scanPage).also { sensors.start(state.scanPage) }
            ButtonGroup.Comm -> {
                sensors.showPage(COMM_PAGES.getValue(state.commStatus))
                viewer.show(ViewerPage.LogsConsole)
            }
            ButtonGroup.Mode -> viewer.show(ViewerPage.ModeWindow)
            else -> GROUP_PAGES[group]?.let { (sensorPage, viewerPage) ->
                sensors.showPage(sensorPage)
                viewer.show(viewerPage)
            }
        }
    }

    private fun alert(sound: TrekSound, spoken: String, echo: String) {
        ctx.speakIfChatty(spoken)
        ctx.play(sound)
        ctx.sendCommand(echo)
    }

    private fun openScreen(screen: Screen) {
        when (screen) {
            Screen.Client -> open(screen, "Tryclient", "Client")
            Screen.Crew -> open(screen, "Access Starship Crew", "Crew information and evaluation")
            Screen.Inventory -> open(screen, "Access Starship Inventory", "Inventory")
            else -> clicked { navigate(screen) }
        }
    }

    private fun open(screen: Screen, message: String, spoken: String) = clicked {
        ctx.say(message)
        ctx.speakIfChatty(spoken)
        navigate(screen)
    }

    private fun openMap() = clicked {
        val location = ctx.state.readings.location
        val lat = location?.latitude ?: 0.0
        val lon = location?.longitude ?: 0.0
        ctx.say("Open planetary mapping")
        ctx.say("geo:$lat,$lon")
        if (!services.launcher.openMap(lat, lon)) ctx.say("No application for mapping.")
    }

    private fun chatSend(text: String) = clicked {
        ctx.sendText(text)
        ctx.sayChat(text)
    }

    /** buttonsound() then the action, like most button handlers of the original. */
    private inline fun clicked(action: () -> Unit) {
        ctx.buttonSound()
        action()
    }

    override fun onCleared() {
        lifecycle.stopAll()
        super.onCleared()
    }

    private companion object {
        fun initialState(services: PlatformServices): TrycorderState {
            val settings = services.settings.current
            return TrycorderState(
                peersText = services.systemInfo.ipAddress(),
                speakLanguage = settings.speakLanguage,
                listenLanguage = settings.listenLanguage,
            )
        }

        val COMM_PAGES = mapOf(
            CommStatus.Closed to SensorPage.StartrekLogo,
            CommStatus.Hailing to SensorPage.AudioWave,
            CommStatus.Intercom to SensorPage.Walkie,
            CommStatus.Chat to SensorPage.Chat,
        )

        val GROUP_PAGES = mapOf(
            ButtonGroup.Shield to (SensorPage.Shields to ViewerPage.TractorBeam),
            ButtonGroup.Fire to (SensorPage.Fire to ViewerPage.FireControl),
            ButtonGroup.Transporter to (SensorPage.Transporter to ViewerPage.TransporterSeek),
            ButtonGroup.Tractor to (SensorPage.Tractor to ViewerPage.TractorBeam),
            ButtonGroup.Motor to (SensorPage.Motor to ViewerPage.Animate),
        )
    }
}
