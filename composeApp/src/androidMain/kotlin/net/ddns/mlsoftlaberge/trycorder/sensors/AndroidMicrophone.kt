package net.ddns.mlsoftlaberge.trycorder.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * AudioRecord at 8 kHz mono 16 bit with the minimum buffer, read 1024 samples at a time, like
 * AudSensorView. Emits nothing while RECORD_AUDIO is denied.
 */
internal class AndroidMicrophone(private val context: Context) {
    fun flow(): Flow<ShortArray> = flow {
        val record = openRecorder() ?: return@flow
        try {
            record.startRecording()
            val buffer = ShortArray(PcmDecimator.AUDIO_CHUNK)
            var read = 0
            while (currentCoroutineContext().isActive && read >= 0) {
                read = record.read(buffer, 0, buffer.size)
                if (read > 0) emit(buffer.copyOf(read))
            }
        } finally {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    // RECORD_AUDIO is checked right before the constructor.
    @SuppressLint("MissingPermission")
    private fun openRecorder(): AudioRecord? {
        val size = AudioRecord.getMinBufferSize(PcmDecimator.AUDIO_SAMPLE_RATE, CHANNEL, ENCODING)
        if (!AndroidSensorHub.granted(context, Manifest.permission.RECORD_AUDIO) || size <= 0) return null
        val record = AudioRecord(MediaRecorder.AudioSource.MIC, PcmDecimator.AUDIO_SAMPLE_RATE, CHANNEL, ENCODING, size)
        val usable = record.state == AudioRecord.STATE_INITIALIZED
        if (!usable) record.release()
        return record.takeIf { usable }
    }

    private companion object {
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
