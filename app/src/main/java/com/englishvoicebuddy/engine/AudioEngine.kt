package com.englishvoicebuddy.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine(
    private val sampleRate: Int = 24000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) {
    private var audioTrack: AudioTrack? = null

    // 不再共享 AudioRecord — 每次录音自管理实例，防止快速连点覆盖导致泄漏+崩溃
    private val bufferSize: Int
        get() = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2400)

    fun startRecording(): Flow<ByteArray> = flow {
        val isRecording = AtomicBoolean(true)
        val size = bufferSize
        var record: AudioRecord? = null

        try {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, size
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                throw IllegalStateException("麦克风不可用")
            }
            record.startRecording()

            val buffer = ByteArray(size)
            while (isRecording.get()) {
                val read = record.read(buffer, 0, size)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                } else if (read < 0) {
                    break
                }
            }
        } finally {
            try { record?.stop() } catch (_: Exception) {}
            try { record?.release() } catch (_: Exception) {}
        }
    }

    // 不再需要 stopRecording — 由 calling coroutine cancel 控制

    fun initPlayer() {
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(audioFormat)
                .build(),
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat),
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
    }

    suspend fun playChunk(data: ByteArray) = withContext(Dispatchers.IO) {
        audioTrack?.write(data, 0, data.size)
    }

    fun release() {
        val track = audioTrack
        audioTrack = null
        track?.stop()
        track?.release()
    }
}
