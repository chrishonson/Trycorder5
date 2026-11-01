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
import net.ddns.mlsoftlaberge.trycorder.link.LinkEvent
import net.ddns.mlsoftlaberge.trycorder.voice.TrekCommand
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceCommands
import net.ddns.mlsoftlaberge.trycorder.voice.VoiceMatch

/**
 * Store of the main Trycorder screen (the old TrycorderFragment).
 * [navigate] opens another screen (gallery, settings, crew...).
 */
class TrycorderStore(
    private val services: PlatformServices,
    private val navigate: (Screen) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<TrycorderState> = _state.asStateFlow()

    private val ctx = TrycorderContext(services, viewModelScope, _state)
    private val sensors = SensorActions(ctx)
    private val viewer = ViewerActions(ctx, sensors)
    private val commands = CommandActions(ctx, sensors, viewer)
    private val dynamic = DynamicStatus(services.keyValueStore)

    init {
        viewModelScope.launch {
            services.listener.results.collect { candidates ->
                VoiceCommands.pickCandidate(candidates)?.let { understood(it) }
            }
        }
        viewModelScope.launch {
            services.link.events.collect { onLinkEvent(it) }
        }
    }

    fun dispatch(intent: TrycorderIntent) {
        when (intent) {
            is TrycorderIntent.Lifecycle -> onLifecycle(intent)
            is TrycorderIntent.Bar -> onBar(intent)
            is TrycorderIntent.Control -> onControl(intent)
            is TrycorderIntent.Slider -> onSlider(intent)
            is TrycorderIntent.Comm -> onComm(intent)
        }
    }

    private fun initialState(): TrycorderState {
        val settings = services.settings.current
        return TrycorderState(
            peersText = services.systemInfo.ipAddress(),
            speakLanguage = settings.speakLanguage,
            listenLanguage = settings.listenLanguage,
        )
    }

    private fun onLifecycle(intent: TrycorderIntent.Lifecycle) {
        when (intent) {
            TrycorderIntent.Start -> startServer()
            TrycorderIntent.Stop -> if (services.settings.current.autoStop) stopServer()
            TrycorderIntent.Resume -> resume()
            TrycorderIntent.Pause -> pause()
        }
    }

    private fun onBar(intent: TrycorderIntent.Bar) {
        when (intent) {
            TrycorderIntent.Talk -> listen()
            TrycorderIntent.StartSensors -> ctx.buttonSound().also { sensors.start(ctx.state.sensorPage) }
            TrycorderIntent.StopSensors -> ctx.buttonSound().also { sensors.stop() }
            TrycorderIntent.ToggleSound -> sensors.toggleSound()
            TrycorderIntent.OpenSettings -> open(Screen.Settings, "Settings", "Settings")
            TrycorderIntent.Snap -> commands.execute(TrekCommand.SnapPhoto, fromVoice = false)
            TrycorderIntent.TakePhoto -> ctx.buttonSound().also { viewer.takePhoto() }
            TrycorderIntent.RecordVideo -> ctx.buttonSound().also { viewer.recordVideo() }
            TrycorderIntent.OpenGallery -> ctx.buttonSound().also { navigate(Screen.PhotoGallery) }
        }
    }

    private fun onControl(intent: TrycorderIntent.Control) {
        when (intent) {
            is TrycorderIntent.SelectGroup -> selectGroup(intent.group)
            is TrycorderIntent.Command -> commands.execute(intent.command, fromVoice = false)
            TrycorderIntent.YellowAlert -> alert(TrekSound.Alert, "Yellow ALERT !", "yellow alert")
            TrycorderIntent.RedAlert -> alert(TrekSound.RedAlert, "RED ALERT !", "red alert")
            TrycorderIntent.NextPlan -> ctx.buttonSound().also { viewer.nextPlan() }
            TrycorderIntent.FederationLogo -> ctx.buttonSound()
            TrycorderIntent.OpenMap -> openMap()
            is TrycorderIntent.OpenScreen -> openScreen(intent.screen)
        }
    }

    private fun onSlider(intent: TrycorderIntent.Slider) {
        val sliders = SliderReducer
        when (intent) {
            is TrycorderIntent.FireDirection -> ctx.update { sliders.fireDirection(it, intent.progress) }
            is TrycorderIntent.FireForce -> ctx.update { sliders.fireForce(it, intent.progress) }
            is TrycorderIntent.TransporterSlideStart -> {
                val beamingDown = ctx.state.transporterSliders[intent.index].progress == 0
                ctx.play(if (beamingDown) TrekSound.BeamOut else TrekSound.BeamIn)
                ctx.update { sliders.transporterStart(it, intent.index) }
            }
            is TrycorderIntent.TransporterSlide -> ctx.update { sliders.transporterMove(it, intent.index, intent.progress) }
            is TrycorderIntent.TransporterSlideStop -> ctx.update { sliders.transporterStop(it, intent.index) }
            is TrycorderIntent.BeamRotate -> ctx.update { sliders.beam(it, it.beamControls.copy(rotate = intent.rotate)) }
            is TrycorderIntent.BeamFreq -> ctx.update { sliders.beam(it, it.beamControls.copy(freq = intent.freq)) }
            is TrycorderIntent.BeamForce -> ctx.update { sliders.beam(it, it.beamControls.copy(force = intent.force)) }
        }
    }

    private fun onComm(intent: TrycorderIntent.Comm) {
        when (intent) {
            TrycorderIntent.PushToTalkDown -> ctx.say("start streaming audio").also { services.link.startAudioStream() }
            TrycorderIntent.PushToTalkUp -> ctx.say("stop streaming audio").also { services.link.stopAudioStream() }
            TrycorderIntent.CommandWindow -> ctx.buttonSound().also { viewer.show(ViewerPage.CommandControl) }
            TrycorderIntent.ServerOn -> ctx.buttonSound().also { startServer() }
            TrycorderIntent.ServerOff -> ctx.buttonSound().also { stopServer() }
            is TrycorderIntent.ChatInput -> ctx.update { it.copy(chatInput = intent.text) }
            TrycorderIntent.ChatSend -> chatSend(ctx.state.chatInput).also { ctx.update { it.copy(chatInput = "") } }
            is TrycorderIntent.ChatRequest -> chatSend(intent.request)
            is TrycorderIntent.CommandInput -> ctx.update { it.copy(commandInput = intent.text) }
            TrycorderIntent.CommandSend -> {
                ctx.buttonSound()
                ctx.sendCommand(ctx.state.commandInput)
                ctx.update { it.copy(commandInput = "") }
            }
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
            else -> ctx.buttonSound().also { navigate(screen) }
        }
    }

    private fun open(screen: Screen, message: String, spoken: String) {
        ctx.buttonSound()
        ctx.say(message)
        ctx.speakIfChatty(spoken)
        navigate(screen)
    }

    private fun openMap() {
        ctx.buttonSound()
        val location = ctx.state.readings.location
        val lat = location?.latitude ?: 0.0
        val lon = location?.longitude ?: 0.0
        ctx.say("Open planetary mapping")
        ctx.say("geo:$lat,$lon")
        if (!services.launcher.openMap(lat, lon)) ctx.say("No application for mapping.")
    }

    private fun listen() {
        ctx.update { it.copy(topStatus = "") }
        services.listener.listen(ctx.state.listenLanguage)
    }

    private fun chatSend(text: String) {
        ctx.buttonSound()
        ctx.sendText(text)
        ctx.sayChat(text)
    }

    /** understood(): a sentence from the microphone. */
    private fun understood(text: String) {
        ctx.update { it.copy(topStatus = text) }
        if (matchVoice(text)) {
            ctx.say("Said: $text")
            ctx.sendText(text)
            if (services.settings.current.autoListen) listen()
        } else {
            ctx.say("Understood: $text")
            ctx.sendText(text)
            ctx.speak(text)
        }
    }

    /** matchvoice(): act on a sentence, true when it was a Trycorder command. */
    private fun matchVoice(text: String): Boolean {
        when (val match = VoiceCommands.parse(text) ?: return false) {
            is VoiceMatch.SwitchLanguage -> {
                ctx.update { it.copy(speakLanguage = match.language, listenLanguage = match.language) }
                ctx.speak(if (match.language.code == "FR") "français" else "english", match.language)
            }
            VoiceMatch.Rude -> {
                if (ctx.state.speakLanguage.code == "FR") {
                    ctx.speak("Ne m'adressez pas la parole de cette façon")
                } else {
                    ctx.play(TrekSound.DoNotAddress)
                }
                viewer.show(ViewerPage.FederationLogo)
                sensors.showPage(SensorPage.EarthStill)
                viewer.showGroup(ButtonGroup.None)
            }
            is VoiceMatch.Command -> commands.execute(match.command, fromVoice = true)
        }
        return true
    }

    private fun onLinkEvent(event: LinkEvent) {
        when (event) {
            is LinkEvent.PeersChanged -> ctx.update { it.copy(peersText = PeerFormat.format(event.peers)) }
            is LinkEvent.StatsChanged -> with(event.stats) {
                ctx.sayChat("Trycorders=$trycorders")
                ctx.sayChat("Countrys=$countries")
                ctx.sayChat("States=$states")
                ctx.sayChat("Citys=$cities")
            }
            is LinkEvent.Logs -> {
                ctx.update { it.copy(topStatus = event.text) }
                ctx.say("Logs: ${event.text}")
                ctx.sayChat(event.text)
            }
            is LinkEvent.Text -> {
                ctx.update { it.copy(topStatus = event.text) }
                ctx.say("Received: ${event.text}")
                if (!matchVoice(event.text)) ctx.sayChat(event.text)
            }
            is LinkEvent.Status -> Unit
        }
    }

    private fun startServer() {
        ctx.say("Start Trycorder Service")
        services.link.start()
        ctx.update { it.copy(peersText = PeerFormat.format(services.link.peers.value, services.systemInfo.ipAddress())) }
    }

    private fun stopServer() {
        ctx.say("Stop Trycorder Service")
        services.link.stop()
    }

    /** onResume(): restore the last pages and restart the scanning sensor. */
    private fun resume() {
        val saved = dynamic.load()
        ctx.update { it.copy(soundEnabled = saved.soundEnabled, commStatus = saved.commStatus, viewerFront = saved.viewerFront) }
        viewer.showGroup(saved.buttonGroup)
        sensors.showPage(saved.scanPage)
        sensors.showPage(saved.sensorPage)
        viewer.show(saved.viewerPage)
        if (saved.sensorPage.isScanPage) sensors.start(saved.sensorPage)
        ctx.update {
            it.copy(
                fireDirection = 0,
                fireForce = TrycorderState.DEFAULT_FIRE_FORCE,
                peersText = PeerFormat.format(services.link.peers.value, services.systemInfo.ipAddress()),
            )
        }
    }

    /** onPause(): save the pages and stop sensors, camera and stats. */
    private fun pause() {
        dynamic.save(ctx.state)
        sensors.stop()
        viewer.stopStats()
    }

    override fun onCleared() {
        sensors.stop()
        viewer.stopStats()
        super.onCleared()
    }

    private companion object {
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
