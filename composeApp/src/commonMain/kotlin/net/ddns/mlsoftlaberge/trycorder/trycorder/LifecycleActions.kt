package net.ddns.mlsoftlaberge.trycorder.trycorder

/** Screen lifecycle and the network service (onStart/onStop/onResume/onPause of the fragment). */
internal class LifecycleActions(
    private val ctx: TrycorderContext,
    private val sensors: SensorActions,
    private val viewer: ViewerActions,
) {
    private val dynamic = DynamicStatus(ctx.services.keyValueStore)

    fun startServer() {
        ctx.say("Start Trycorder Service")
        ctx.services.link.start()
        refreshPeers()
    }

    fun stopServer() {
        ctx.say("Stop Trycorder Service")
        ctx.services.link.stop()
    }

    /** onResume(): restore the last pages and restart the scanning sensor. */
    fun resume() {
        val saved = dynamic.load()
        ctx.update {
            it.copy(soundEnabled = saved.soundEnabled, commStatus = saved.commStatus, viewerFront = saved.viewerFront)
        }
        viewer.showGroup(saved.buttonGroup)
        sensors.showPage(saved.scanPage)
        sensors.showPage(saved.sensorPage)
        viewer.show(saved.viewerPage)
        if (saved.sensorPage.isScanPage) sensors.start(saved.sensorPage)
        ctx.update { it.copy(fireDirection = 0, fireForce = TrycorderState.DEFAULT_FIRE_FORCE) }
        refreshPeers()
    }

    /** onPause(): save the pages and stop sensors and stats. */
    fun pause() {
        dynamic.save(ctx.state)
        stopAll()
    }

    fun stopAll() {
        sensors.stop()
        viewer.stopStats()
    }

    private fun refreshPeers() = ctx.update {
        it.copy(peersText = PeerFormat.format(ctx.services.link.peers.value, ctx.services.systemInfo.ipAddress()))
    }
}
