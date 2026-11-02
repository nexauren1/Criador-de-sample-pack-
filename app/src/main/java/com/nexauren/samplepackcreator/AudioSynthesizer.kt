package com.nexauren.samplepackcreator

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline sample synthesizer. Every output is newly generated PCM audio,
 * not a renamed or duplicated source file.
 */
object AudioSynthesizer {
    val soundTypes = listOf("Kick", "Snare", "Clap", "Hi-Hat", "Bass", "Percussion", "Synth", "FX", "Pluck", "Tom")

    fun generate(
        soundType: String,
        durationMs: Int = 700,
        sampleRate: Int = 44100,
        variation: Int = 0,
        seed: Long = System.nanoTime()
    ): ByteArray {
        val rate = sampleRate.coerceIn(22050, 96000)
        val length = durationMs.coerceIn(80, 6000)
        val frames = max(1, rate * length / 1000)
        val random = Random(seed + variation * 7919L)
        val output = FloatArray(frames)
        val type = soundType.lowercase()
        val detune = 0.92 + (variation % 13) * 0.013
        val pitchOffset = (variation % 11) * 3.5

        var phase = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var filterState = 0.0
        var previousNoise = 0.0

        for (i in 0 until frames) {
            val t = i.toDouble() / rate
            val normalized = i.toDouble() / frames
            val noise = random.nextDouble() * 2.0 - 1.0
            val sample = when {
                type.contains("kick") || type.contains("808") -> {
                    val start = 125.0 + pitchOffset * 2.0
                    val base = 39.0 + (variation % 7) * 2.5
                    val frequency = base + start * exp(-t * (22.0 + (variation % 5) * 3.0))
                    phase += 2.0 * PI * frequency / rate
                    val body = sin(phase) * exp(-t * (5.5 + (variation % 6) * 0.45))
                    val click = noise * exp(-t * 105.0) * (0.06 + (variation % 4) * 0.018)
                    body * 0.95 + click
                }
                type.contains("snare") -> {
                    phase += 2.0 * PI * (175.0 + pitchOffset * 2.0) / rate
                    val tone = sin(phase) * exp(-t * (19.0 + variation % 7))
                    val high = noise - previousNoise * 0.82
                    previousNoise = noise
                    val snap = exp(-t * (9.0 + variation % 8)) * high
                    tone * 0.34 + snap * 0.52
                }
                type.contains("clap") -> {
                    val spacing = 0.012 + (variation % 5) * 0.003
                    val burst1 = exp(-abs(t) * 45.0)
                    val burst2 = if (t >= spacing) exp(-(t - spacing) * 55.0) else 0.0
                    val burst3 = if (t >= spacing * 2.2) exp(-(t - spacing * 2.2) * 62.0) else 0.0
                    val high = noise - previousNoise * 0.72
                    previousNoise = noise
                    high * (burst1 * 0.32 + burst2 * 0.25 + burst3 * 0.18) * exp(-t * 2.8)
                }
                type.contains("hat") || type.contains("shaker") -> {
                    val decay = exp(-t * (22.0 + (variation % 8) * 5.0))
                    val metal = sin(2.0 * PI * (3120.0 + pitchOffset * 5.0) * t) * 0.32 +
                        sin(2.0 * PI * (4870.0 + pitchOffset * 7.0) * t) * 0.22 +
                        sin(2.0 * PI * (6380.0 + pitchOffset * 9.0) * t) * 0.16
                    val high = noise - previousNoise * 0.91
                    previousNoise = noise
                    (metal + high * 0.48) * decay
                }
                type.contains("bass") || type.contains("sub") -> {
                    val frequency = 48.0 + pitchOffset + 14.0 * sin(2.0 * PI * 0.23 * t)
                    phase += 2.0 * PI * frequency / rate
                    val saw = 2.0 * (phase / (2.0 * PI) - kotlin.math.floor(phase / (2.0 * PI))) - 1.0
                    filterState += (saw - filterState) * (0.018 + (variation % 6) * 0.006)
                    filterState * (0.78 + 0.08 * sin(2.0 * PI * 0.4 * t))
                }
                type.contains("tom") -> {
                    val frequency = 105.0 + pitchOffset + 90.0 * exp(-t * 13.0)
                    phase += 2.0 * PI * frequency / rate
                    sin(phase) * exp(-t * (7.0 + variation % 5))
                }
                type.contains("perc") || type.contains("bongo") -> {
                    val frequency = 210.0 + pitchOffset * 3.0 + 60.0 * exp(-t * 20.0)
                    phase += 2.0 * PI * frequency / rate
                    phase2 += 2.0 * PI * (frequency * 1.47) / rate
                    (sin(phase) * 0.55 + sin(phase2) * 0.22 + noise * 0.10) * exp(-t * 12.0)
                }
                type.contains("pluck") -> {
                    val frequency = 230.0 + pitchOffset * 4.0
                    phase += 2.0 * PI * frequency / rate
                    phase2 += 2.0 * PI * frequency * (1.997 + variation % 4 * 0.001) / rate
                    val envelope = exp(-t * (4.5 + variation % 7))
                    (sin(phase) * 0.65 + sin(phase2) * 0.25) * envelope
                }
                type.contains("fx") || type.contains("sweep") -> {
                    val frequency = 180.0 + 4200.0 * normalized * normalized + pitchOffset * 3.0
                    phase += 2.0 * PI * frequency / rate
                    val filteredNoise = noise - previousNoise * 0.86
                    previousNoise = noise
                    (sin(phase) * 0.42 + filteredNoise * 0.42) * (1.0 - normalized * 0.35)
                }
                else -> {
                    val base = 220.0 + pitchOffset * 2.0
                    phase += 2.0 * PI * base / rate
                    phase2 += 2.0 * PI * base * detune / rate
                    phase3 += 2.0 * PI * base * 2.01 / rate
                    val attack = min(1.0, t / 0.025)
                    val release = min(1.0, (length / 1000.0 - t) / 0.12)
                    val envelope = max(0.0, min(attack, release)) * (0.68 + 0.16 * sin(2.0 * PI * 4.0 * t))
                    (sin(phase) * 0.48 + sin(phase2) * 0.34 + sin(phase3) * 0.16) * envelope
                }
            }
            val variationGain = 0.88 + (variation % 9) * 0.025
            val soft = sample * variationGain
            output[i] = (soft / (1.0 + abs(soft) * 0.25)).coerceIn(-0.98, 0.98).toFloat()
        }

        // A subtle fade avoids clicks at the end of tonal sounds.
        val fadeFrames = min(frames / 5, rate / 30)
        if (fadeFrames > 0) {
            for (i in 0 until fadeFrames) {
                val gain = i.toFloat() / fadeFrames
                output[frames - fadeFrames + i] *= gain
            }
        }
        return toWav16(output, rate)
    }

    private fun toWav16(samples: FloatArray, sampleRate: Int): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArrayOutputStream(44 + dataSize)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)
        out.write(header.array())
        samples.forEach { sample ->
            val value = (sample.coerceIn(-1f, 1f) * 32767f).toInt()
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }
        return out.toByteArray()
    }
}
