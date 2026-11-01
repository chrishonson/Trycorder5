package net.ddns.mlsoftlaberge.trycorder.trycorder

/** Pure state changes of the fire, transporter and beam sliders. */
internal object SliderReducer {
    private const val FIRE_CENTER = 50

    fun fireDirection(state: TrycorderState, progress: Int) = state.copy(fireDirection = progress - FIRE_CENTER)

    fun fireForce(state: TrycorderState, progress: Int) = state.copy(fireForce = progress)

    /** onStartTrackingTouch: a slider at 0 beams down (mode 2), otherwise beams up (mode 1). */
    fun transporterStart(state: TrycorderState, index: Int): TrycorderState {
        val slider = state.transporterSliders[index]
        val mode = if (slider.progress == 0) 2 else 1
        return state.copy(
            transporterMode = mode,
            transporterSliders = state.transporterSliders.replace(index, slider.copy(mode = mode)),
        )
    }

    fun transporterMove(state: TrycorderState, index: Int, progress: Int): TrycorderState {
        val slider = state.transporterSliders[index].copy(progress = progress)
        return state.copy(
            transporterSliders = state.transporterSliders.replace(index, slider),
            transporterTextUp = "Transport ${index + 1}: $progress/${TrycorderState.TRANSPORTER_MAX}",
            transporterPosition = position(slider),
        )
    }

    fun transporterStop(state: TrycorderState, index: Int): TrycorderState {
        val slider = state.transporterSliders[index]
        val done = slider.progress == 0 || slider.progress == TrycorderState.TRANSPORTER_MAX
        val outcome = if (done) "Complete" else "Failed"
        return state.copy(
            transporterPosition = position(slider),
            transporterTextDown = "Transport ${index + 1} $outcome: ${slider.progress}/${TrycorderState.TRANSPORTER_MAX}",
        )
    }

    /** The shared beam controls drive the tractor beam or the shields, whichever group is shown. */
    fun beam(state: TrycorderState, controls: BeamParams): TrycorderState = when (state.buttonGroup) {
        ButtonGroup.Tractor -> state.copy(beamControls = controls, tractor = controls)
        ButtonGroup.Shield -> state.copy(beamControls = controls, shield = controls)
        else -> state.copy(beamControls = controls)
    }

    private fun position(slider: TransporterSlider) =
        if (slider.mode == 2) slider.progress else TrycorderState.TRANSPORTER_MAX - slider.progress

    private fun <T> List<T>.replace(index: Int, value: T): List<T> = mapIndexed { i, v -> if (i == index) value else v }
}
