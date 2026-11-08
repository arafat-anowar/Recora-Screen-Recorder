package com.veylqo.recora.service.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class InternalAudioRecorder {

    companion object {
        private const val TAG = "InternalAudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 192000
        private const val TIMEOUT_US = 10000L
    }

    private var audioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var audioTrackIndex = -1
    private var isMuxerStarted = false

    private var recordThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun start(
        mediaProjection: MediaProjection,
        outputFile: File,
        includeMic: Boolean = false
    ): Boolean {
        stop()

        try {
            // 1. Build AudioPlaybackCaptureConfiguration for internal app & game sounds
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize * 4, 8192)

            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(captureConfig)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize with AudioPlaybackCaptureConfiguration")
                record.release()
                return false
            }
            audioRecord = record

            // Optional: Secondary AudioRecord for microphone if user wants Internal + Mic mixed
            if (includeMic) {
                try {
                    val micRecord = AudioRecord(
                        android.media.MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    if (micRecord.state == AudioRecord.STATE_INITIALIZED) {
                        micAudioRecord = micRecord
                    } else {
                        micRecord.release()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not initialize secondary mic record", e)
                }
            }

            // 2. Setup AAC Audio Encoder
            val aacFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLE_RATE,
                CHANNEL_COUNT
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            }

            val aacCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aacCodec.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aacCodec.start()
            encoder = aacCodec

            // 3. Setup MediaMuxer for AAC file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            isMuxerStarted = false
            audioTrackIndex = -1

            isRecording.set(true)
            audioRecord?.startRecording()
            micAudioRecord?.startRecording()

            // 4. Background Encoding Loop
            recordThread = Thread({
                processAudioCapture(bufferSize)
            }, "RecoraInternalAudioCapture").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.i(TAG, "Started pure internal audio recording into ${outputFile.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start internal audio capture", e)
            cleanUp()
            return false
        }
    }

    private fun processAudioCapture(bufferSize: Int) {
        val pcmBuffer = ByteArray(bufferSize)
        val micBuffer = if (micAudioRecord != null) ByteArray(bufferSize) else null
        val bufferInfo = MediaCodec.BufferInfo()
        var presentationTimeUs = 0L
        val startTimeNs = System.nanoTime()

        val record = audioRecord ?: return
        val mic = micAudioRecord
        val codec = encoder ?: return
        val mux = muxer ?: return

        while (isRecording.get()) {
            val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
            if (bytesRead <= 0) continue

            // If mixing microphone audio with internal audio:
            if (mic != null && micBuffer != null) {
                val micBytes = mic.read(micBuffer, 0, minOf(bytesRead, micBuffer.size))
                if (micBytes > 0) {
                    mixPcm16Stereo(pcmBuffer, micBuffer, minOf(bytesRead, micBytes))
                }
            }

            // Feed PCM into MediaCodec encoder
            var inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            while (inputBufferIndex < 0 && isRecording.get()) {
                drainEncoder(codec, mux, bufferInfo, false)
                inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            }

            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(pcmBuffer, 0, bytesRead)
                    presentationTimeUs = (System.nanoTime() - startTimeNs) / 1000L
                    codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                }
            }

            drainEncoder(codec, mux, bufferInfo, false)
        }

        // Send End-Of-Stream to AAC encoder
        try {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                codec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(codec, mux, bufferInfo, true)
        } catch (e: Exception) {
            Log.w(TAG, "Error finalizing encoder", e)
        }
    }

    private fun drainEncoder(codec: MediaCodec, mux: MediaMuxer, bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break else continue
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!isMuxerStarted) {
                    val newFormat = codec.outputFormat
                    audioTrackIndex = mux.addTrack(newFormat)
                    mux.start()
                    isMuxerStarted = true
                    Log.i(TAG, "MediaMuxer started with AAC format: $newFormat")
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputBufferIndex)
                if (encodedData != null && isMuxerStarted && audioTrackIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                        mux.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    /**
     * Cleanly mix two 16-bit PCM buffers with saturation clamping to prevent digital clipping.
     */
    private fun mixPcm16Stereo(srcDest: ByteArray, mic: ByteArray, length: Int) {
        for (i in 0 until (length - 1) step 2) {
            val s1 = ((srcDest[i + 1].toInt() shl 8) or (srcDest[i].toInt() and 0xFF)).toShort()
            val s2 = ((mic[i + 1].toInt() shl 8) or (mic[i].toInt() and 0xFF)).toShort()

            var mixed = s1.toInt() + s2.toInt()
            if (mixed > 32767) mixed = 32767
            if (mixed < -32768) mixed = -32768

            srcDest[i] = (mixed and 0xFF).toByte()
            srcDest[i + 1] = ((mixed shr 8) and 0xFF).toByte()
        }
    }

    fun stop() {
        isRecording.set(false)
        try {
            recordThread?.join(1500)
        } catch (e: Exception) {}
        recordThread = null
        cleanUp()
    }

    private fun cleanUp() {
        try { audioRecord?.stop() } catch (e: Exception) {}
        try { audioRecord?.release() } catch (e: Exception) {}
        audioRecord = null

        try { micAudioRecord?.stop() } catch (e: Exception) {}
        try { micAudioRecord?.release() } catch (e: Exception) {}
        micAudioRecord = null

        try { encoder?.stop() } catch (e: Exception) {}
        try { encoder?.release() } catch (e: Exception) {}
        encoder = null

        try {
            if (isMuxerStarted) {
                muxer?.stop()
            }
            muxer?.release()
        } catch (e: Exception) {}
        muxer = null
        isMuxerStarted = false
        audioTrackIndex = -1
    }

    /**
     * Merge video track from tempVideoFile and pure AAC audio track from tempAudioFile
     * directly into the final destination FileDescriptor using MediaExtractor and MediaMuxer.
     * Takes ~200ms without any video re-encoding.
     */
    fun mergeVideoAndAudio(videoFile: File, audioFile: File, targetFd: FileDescriptor): Boolean {
        var muxer: MediaMuxer? = null
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null

        return try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val vTrackIndex = (0 until videoExtractor.trackCount).firstOrNull {
                videoExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: -1

            if (vTrackIndex == -1) {
                Log.e(TAG, "No video track found in ${videoFile.name}")
                return false
            }

            muxer = MediaMuxer(targetFd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val vFormat = videoExtractor.getTrackFormat(vTrackIndex)
            val muxerVTrack = muxer.addTrack(vFormat)

            var muxerATrack = -1
            var aTrackIndex = -1

            if (audioFile.exists() && audioFile.length() > 0) {
                try {
                    audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
                    aTrackIndex = (0 until audioExtractor.trackCount).firstOrNull {
                        audioExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                    } ?: -1

                    if (aTrackIndex != -1) {
                        val aFormat = audioExtractor.getTrackFormat(aTrackIndex)
                        muxerATrack = muxer.addTrack(aFormat)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not open audio track for merging", e)
                }
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // 1. Copy Video Samples
            videoExtractor.selectTrack(vTrackIndex)
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(muxerVTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // 2. Copy Audio Samples (Pure Internal Audio)
            if (audioExtractor != null && aTrackIndex != -1 && muxerATrack != -1) {
                audioExtractor.selectTrack(aTrackIndex)
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(muxerATrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            Log.i(TAG, "Successfully merged video and pure internal audio into destination")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed merging video and audio", e)
            false
        } finally {
            try { videoExtractor?.release() } catch (e: Exception) {}
            try { audioExtractor?.release() } catch (e: Exception) {}
            try { muxer?.release() } catch (e: Exception) {}
        }
    }
}
