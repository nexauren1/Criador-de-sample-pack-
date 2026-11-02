package com.nexauren.samplepackcreator

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Uses the Android platform decoders (MediaExtractor + MediaCodec) to normalize
 * supported compressed or PCM audio into mono/stereo 16-bit WAV.
 * Codec support depends on the Android device and the source encoding.
 */
object AudioDecoder {
    fun decodeToWav(context: Context, uri: Uri): ByteArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var codecStarted = false
        try {
            extractor.setDataSource(context, uri, null)
            var selectedTrack = -1
            var sourceFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    selectedTrack = index
                    sourceFormat = format
                    break
                }
            }
            if (selectedTrack < 0 || sourceFormat == null) return null
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(sourceFormat, null, null, 0)
            codec.start()
            codecStarted = true
            extractor.selectTrack(selectedTrack)

            val pcm = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var sampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var sourceChannels = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var outputChannels = sourceChannels.coerceIn(1, 2)
            var outputEncoding = AudioFormat.ENCODING_PCM_16BIT
            var idleLoops = 0
            val maxOutputBytes = 160 * 1024 * 1024

            while (!outputEnded) {
                var madeProgress = false
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("O descodificador não disponibilizou um buffer de entrada.")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                extractor.sampleFlags
                            )
                            extractor.advance()
                        }
                        madeProgress = true
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outputIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && info.size > 0 &&
                            info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            val format = codec.outputFormat
                            sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                            sourceChannels = format.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, sourceChannels)
                            outputChannels = sourceChannels.coerceIn(1, 2)
                            outputEncoding = format.getIntegerOrDefault(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)

                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            appendAsPcm16(
                                outputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN),
                                sourceChannels,
                                outputChannels,
                                outputEncoding,
                                pcm
                            )
                            if (pcm.size() > maxOutputBytes) {
                                throw IllegalArgumentException("O áudio descodificado ultrapassa o limite de 160 MB.")
                            }
                        }
                        val end = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (end) outputEnded = true
                        madeProgress = true
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat
                        sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        sourceChannels = format.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, sourceChannels)
                        outputChannels = sourceChannels.coerceIn(1, 2)
                        outputEncoding = format.getIntegerOrDefault(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        madeProgress = true
                    }
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                }

                if (madeProgress) idleLoops = 0 else idleLoops++
                if (idleLoops > 500) throw IllegalStateException("O descodificador deixou de responder.")
            }

            val pcmBytes = pcm.toByteArray()
            if (pcmBytes.isEmpty() || sampleRate <= 0 || outputChannels !in 1..2) return null
            return createWavHeader(pcmBytes, sampleRate, outputChannels)
        } catch (_: Exception) {
            return null
        } finally {
            try {
                if (codecStarted) codec?.stop()
            } catch (_: Exception) {
                // Decoder can already have stopped after an internal codec error.
            }
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun appendAsPcm16(
        input: ByteBuffer,
        sourceChannels: Int,
        outputChannels: Int,
        encoding: Int,
        output: ByteArrayOutputStream
    ) {
        val channels = sourceChannels.coerceAtLeast(1)
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            21 -> 3
            22, AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val frameBytes = channels * bytesPerSample
        val frames = input.remaining() / frameBytes
        val values = ShortArray(channels)
        repeat(frames) {
            for (channel in 0 until channels) {
                val sample = when (encoding) {
                    AudioFormat.ENCODING_PCM_8BIT -> ((input.get().toInt() and 0xFF) - 128) shl 8
                    AudioFormat.ENCODING_PCM_16BIT -> input.short.toInt()
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                        val b0 = input.get().toInt() and 0xFF
                        val b1 = input.get().toInt() and 0xFF
                        val b2 = input.get().toInt()
                        ((b2 shl 16) or (b1 shl 8) or b0) shr 8
                    }
                    AudioFormat.ENCODING_PCM_32BIT -> (input.int shr 16)
                    AudioFormat.ENCODING_PCM_FLOAT -> (input.float.coerceIn(-1f, 1f) * 32767f).toInt()
                    else -> input.short.toInt()
                }
                values[channel] = sample.coerceIn(-32768, 32767).toShort()
            }

            if (outputChannels == 1) {
                var sum = 0L
                values.forEach { sum += it.toLong() }
                writeShort(output, (sum / values.size).toInt())
            } else {
                if (channels == 1) {
                    writeShort(output, values[0].toInt())
                    writeShort(output, values[0].toInt())
                } else if (channels == 2) {
                    writeShort(output, values[0].toInt())
                    writeShort(output, values[1].toInt())
                } else {
                    var left = 0L
                    var right = 0L
                    var leftCount = 0
                    var rightCount = 0
                    values.forEachIndexed { index, value ->
                        if (index % 2 == 0) { left += value; leftCount++ }
                        else { right += value; rightCount++ }
                    }
                    writeShort(output, (left / leftCount.coerceAtLeast(1)).toInt())
                    writeShort(output, (right / rightCount.coerceAtLeast(1)).toInt())
                }
            }
        }
    }

    private fun createWavHeader(pcm: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + pcm.size)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * 2)
        header.putShort((channels * 2).toShort())
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcm.size)
        return header.array() + pcm
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int =
        try { getInteger(key) } catch (_: Exception) { default }

    private fun writeShort(output: ByteArrayOutputStream, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }
}
