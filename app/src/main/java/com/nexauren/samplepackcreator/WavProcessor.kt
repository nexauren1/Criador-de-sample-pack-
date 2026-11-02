package com.nexauren.samplepackcreator

import kotlin.math.max
import kotlin.math.tanh

/**
 * Lightweight offline processor for uncompressed PCM WAV (16-bit).
 * Compressed and unsupported formats return null instead of being presented
 * as processed variations.
 */
object WavProcessor {
    private val effectNames = listOf(
        "Punchy", "Deep", "Distorted", "Reverse",
        "Echo", "LoFi", "Bright", "Warm"
    )

    fun effectName(index: Int): String {
        val base = effectNames[index.mod(effectNames.size)]
        val cycle = index / effectNames.size
        return if (cycle == 0) base else base + "_Alt" + (cycle + 1)
    }

    fun process(input: ByteArray, variationIndex: Int): ByteArray? {
        val wav = parse(input) ?: return null
        val output = input.copyOf()
        val samples = FloatArray(wav.dataLength / 2)
        var cursor = wav.dataOffset
        for (i in samples.indices) {
            samples[i] = readShort(output, cursor).toFloat() / 32768f
            cursor += 2
        }

        val channels = wav.channels
        val sampleRate = wav.sampleRate
        val cycle = variationIndex / effectNames.size
        when (variationIndex.mod(effectNames.size)) {
            0 -> {
                val attackGain = (1.18f + cycle * 0.055f).coerceAtMost(2.1f)
                val bodyGain = (1.04f + cycle * 0.025f).coerceAtMost(1.7f)
                for (i in samples.indices) {
                    val envelope = if (i < channels * sampleRate / 40) attackGain else bodyGain
                    samples[i] = (samples[i] * envelope).coerceIn(-1f, 1f)
                }
            }
            1 -> {
                val previous = FloatArray(channels)
                val coefficient = (0.12f + cycle * 0.055f).coerceAtMost(0.78f)
                for (i in samples.indices) {
                    val channel = i % channels
                    previous[channel] += coefficient * (samples[i] - previous[channel])
                    samples[i] = previous[channel] * (1.08f + cycle * 0.025f)
                }
            }
            2 -> {
                val drive = 2.8f + cycle * 0.85f
                val normalizer = tanh(drive.toDouble()).toFloat()
                for (i in samples.indices) {
                    samples[i] = (tanh((samples[i] * drive).toDouble()).toFloat() / normalizer)
                        .coerceIn(-1f, 1f)
                }
            }
            3 -> {
                val frames = samples.size / channels
                for (frame in 0 until frames / 2) {
                    val opposite = frames - frame - 1
                    for (channel in 0 until channels) {
                        val a = frame * channels + channel
                        val b = opposite * channels + channel
                        val temp = samples[a]
                        samples[a] = samples[b]
                        samples[b] = temp
                    }
                }
            }
            4 -> {
                val delayFrames = max(1, sampleRate * (180 + (cycle.mod(5) * 35)) / 1000)
                val delaySamples = delayFrames * channels
                val feedback = (0.30f + cycle.mod(5) * 0.075f).coerceAtMost(0.62f)
                for (i in delaySamples until samples.size) {
                    samples[i] = (samples[i] + samples[i - delaySamples] * feedback).coerceIn(-1f, 1f)
                }
            }
            5 -> {
                val bits = (7 - cycle.mod(5)).coerceAtLeast(3)
                val levels = ((1 shl bits) - 1).toFloat()
                for (i in samples.indices) {
                    samples[i] = (samples[i] * levels).toInt() / levels
                }
            }
            6 -> {
                val previousInput = FloatArray(channels)
                val previousOutput = FloatArray(channels)
                val coefficient = (0.92f - cycle.mod(5) * 0.075f).coerceAtLeast(0.55f)
                val gain = 1.2f + cycle.mod(4) * 0.12f
                for (i in samples.indices) {
                    val channel = i % channels
                    val current = samples[i]
                    val high = coefficient * (previousOutput[channel] + current - previousInput[channel])
                    previousInput[channel] = current
                    previousOutput[channel] = high
                    samples[i] = (high * gain).coerceIn(-1f, 1f)
                }
            }
            else -> {
                val amount = 0.18f + cycle.mod(6) * 0.16f
                for (i in samples.indices) {
                    val value = samples[i]
                    samples[i] = (value + amount * value * value * value).coerceIn(-1f, 1f)
                }
            }
        }

        val cycleGain = 1f + (cycle.mod(7) * 0.018f)
        cursor = wav.dataOffset
        for (sample in samples) {
            val value = (sample * cycleGain).coerceIn(-1f, 1f)
            writeShort(output, cursor, (value * 32767f).toInt())
            cursor += 2
        }
        return output
    }

    private data class WavInfo(
        val channels: Int,
        val sampleRate: Int,
        val dataOffset: Int,
        val dataLength: Int
    )

    private fun parse(bytes: ByteArray): WavInfo? {
        if (bytes.size < 44 || ascii(bytes, 0, 4) != "RIFF" || ascii(bytes, 8, 4) != "WAVE") return null
        var offset = 12
        var channels = 0
        var sampleRate = 0
        var bits = 0
        var format = 0
        var dataOffset = -1
        var dataLength = 0

        while (offset + 8 <= bytes.size) {
            val chunk = ascii(bytes, offset, 4)
            val size = readInt(bytes, offset + 4).coerceAtLeast(0)
            val body = offset + 8
            if (body + size > bytes.size) break
            when (chunk) {
                "fmt " -> {
                    if (size >= 16) {
                        format = readUnsignedShort(bytes, body)
                        channels = readUnsignedShort(bytes, body + 2)
                        sampleRate = readInt(bytes, body + 4)
                        bits = readUnsignedShort(bytes, body + 14)
                    }
                }
                "data" -> {
                    dataOffset = body
                    dataLength = size
                }
            }
            offset = body + size + (size and 1)
        }

        if (format != 1 || bits != 16 || channels !in 1..2 || sampleRate <= 0) return null
        if (dataOffset < 0 || dataLength < 2 || dataLength % (2 * channels) != 0) return null
        return WavInfo(channels, sampleRate, dataOffset, dataLength)
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readShort(bytes: ByteArray, offset: Int): Int =
        readUnsignedShort(bytes, offset).toShort().toInt()

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            (bytes[offset + 3].toInt() shl 24)

    private fun writeShort(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
