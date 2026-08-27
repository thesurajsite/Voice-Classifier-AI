package com.app.voiceclassifier.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val RECORD_DURATION_SECONDS = 20
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun record(
        durationSeconds: Int,
        onProgress: (secondsElapsed: Int) -> Unit = {}
    ): FloatArray = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )
        audioRecord = record
        val totalSamples = SAMPLE_RATE * durationSeconds
        val pcmShorts = ShortArray(totalSamples)
        var offset = 0

        try {
            record.startRecording()
            val chunk = ShortArray(bufferSize / 2)
            var lastSecond = -1
            while (offset < totalSamples) {
                val read = record.read(chunk, 0, chunk.size)
                if (read > 0) {
                    val toCopy = minOf(read, totalSamples - offset)
                    System.arraycopy(chunk, 0, pcmShorts, offset, toCopy)
                    offset += toCopy
                    val elapsed = offset / SAMPLE_RATE
                    if (elapsed != lastSecond) {
                        lastSecond = elapsed
                        withContext(Dispatchers.Main) { onProgress(elapsed) }
                    }
                    if (read < 0) break
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                    break
                }
            }
        } finally {
            try {
                record.stop()
            } catch (_: Exception) {}
            record.release()
            audioRecord = null
        }

        // Normalize to float [-1, 1]
        FloatArray(pcmShorts.size) { i -> pcmShorts[i] / 32768f }
    }

    suspend fun record20Seconds(
        onProgress: (secondsElapsed: Int) -> Unit = {}
    ): FloatArray = record(RECORD_DURATION_SECONDS, onProgress)

    suspend fun record10Seconds(
        onProgress: (secondsElapsed: Int) -> Unit = {}
    ): FloatArray = record(10, onProgress)

    fun stop() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    /**
     * Utility to record from an existing PCM file or ShortArray if needed.
     */
    fun shortsToFloats(shorts: ShortArray): FloatArray {
        return FloatArray(shorts.size) { shorts[it] / 32768f }
    }
}
