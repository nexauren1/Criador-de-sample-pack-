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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Musical one-shot synthesizer.
 * Presets use different envelopes and harmonic/noise designs. Noise is only
 * used in acoustic/percussive presets. Output is normalized with headroom.
 */
object AudioSynthesizer {
    val soundTypes = listOf("Kick", "Snare", "Clap", "Hi-Hat", "Bass", "Percussion", "Synth", "FX", "Pluck", "Tom")

    private data class NoiseBurst(
        val delay: Double,
        val weight: Double,
        val falloff: Double,
        val random: Random,
        val filter: Biquad
    )

    private class Biquad private constructor(
        private val b0: Double, private val b1: Double, private val b2: Double,
        private val a1: Double, private val a2: Double
    ) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(input: Double): Double {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = input; y2 = y1; y1 = output
            return output
        }

        companion object {
            fun bandPass(sampleRate: Int, frequency: Double, q: Double): Biquad {
                val f = frequency.coerceIn(80.0, sampleRate * 0.44)
                val omega = 2.0 * PI * f / sampleRate
                val alpha = sin(omega) / (2.0 * q.coerceAtLeast(0.2))
                val a0 = 1.0 + alpha
                return Biquad(
                    alpha / a0, 0.0, -alpha / a0,
                    -2.0 * kotlin.math.cos(omega) / a0, (1.0 - alpha) / a0
                )
            }

            fun highPass(sampleRate: Int, frequency: Double, q: Double = 0.707): Biquad {
                val f = frequency.coerceIn(40.0, sampleRate * 0.44)
                val omega = 2.0 * PI * f / sampleRate
                val c = kotlin.math.cos(omega)
                val alpha = sin(omega) / (2.0 * q)
                val a0 = 1.0 + alpha
                return Biquad(
                    (1.0 + c) * 0.5 / a0, -(1.0 + c) / a0, (1.0 + c) * 0.5 / a0,
                    -2.0 * c / a0, (1.0 - alpha) / a0
                )
            }
        }
    }

    fun generate(
        soundType: String,
        durationMs: Int = 700,
        sampleRate: Int = 44100,
        variation: Int = 0,
        seed: Long = System.nanoTime(),
        pitchSemitones: Float = 0f,
        decay: Float = 1f,
        brightness: Float = 1f,
        drive: Float = 1f
    ): ByteArray {
        val rate = sampleRate.coerceIn(22050, 96000)
        val length = durationMs.coerceIn(80, 6000)
        val frames = max(1, rate * length / 1000)
        val type = soundType.lowercase()
        val variationPitch = (((variation % 7) - 3) * 0.35).toFloat()
        val tuning = 2.0.pow((pitchSemitones.coerceIn(-24f, 24f) + variationPitch) / 12.0)
        val decayScale = decay.coerceIn(0.3f, 3.5f).toDouble()
        val brightnessScale = brightness.coerceIn(0.25f, 2f).toDouble()
        val driveScale = drive.coerceIn(0.5f, 5f).toDouble()
        val random = Random(seed + variation.toLong() * 7919L)

        var kickPhase = 0.0
        var kickSubPhase = 0.0
        var snarePhase = 0.0
        var tomPhase = 0.0
        var percPhase1 = 0.0
        var percPhase2 = 0.0
        var bassPhase = 0.0
        var synthPhase1 = 0.0
        var synthPhase2 = 0.0
        var synthPhase3 = 0.0
        var pluckPhase1 = 0.0
        var pluckPhase2 = 0.0
        var fxPhase = 0.0

        val clapBursts = if (type.contains("clap")) {
            val delays = doubleArrayOf(0.0, 0.011, 0.024, 0.039, 0.055)
            val weights = doubleArrayOf(0.42, 0.30, 0.22, 0.15, 0.09)
            val falls = doubleArrayOf(58.0, 68.0, 75.0, 82.0, 90.0)
            delays.indices.map { i ->
                NoiseBurst(
                    delays[i], weights[i], falls[i],
                    Random(seed + 991L * (i + 1) + variation * 3571L),
                    Biquad.bandPass(rate, doubleArrayOf(1250.0, 1700.0, 2150.0, 2700.0, 3300.0)[i], 0.9)
                )
            }
        } else emptyList()

        val snareNoise = Biquad.bandPass(rate, 2100.0, 0.85)
        val hatNoise = Biquad.highPass(rate, 6200.0)
        val kickClick = Biquad.highPass(rate, 1800.0)
        val percNoise = Biquad.bandPass(rate, 1450.0, 0.9)
        val fxNoise = Biquad.bandPass(rate, 3200.0, 0.75)
        val raw = FloatArray(frames)

        for (i in 0 until frames) {
            val t = i.toDouble() / rate
            val u = i.toDouble() / frames
            val noise = random.nextDouble() * 2.0 - 1.0

            val sample = when {
                type.contains("kick") || type.contains("808") -> {
                    val startHz = (148.0 + (variation % 5) * 3.0) * tuning
                    val rootHz = (47.0 + (variation % 4) * 1.2) * tuning
                    val frequency = rootHz + startHz * exp(-t * (29.0 / decayScale))
                    kickPhase += 2.0 * PI * frequency / rate
                    kickSubPhase += 2.0 * PI * frequency * 0.5 / rate
                    val body = sin(kickPhase) * exp(-t * (5.8 / decayScale)) * 0.88
                    val sub = sin(kickSubPhase) * exp(-t * (5.0 / decayScale)) * 0.22
                    val click = kickClick.process(noise) * exp(-t * 165.0) * 0.16
                    body + sub + click
                }

                type.contains("snare") -> {
                    snarePhase += 2.0 * PI * (188.0 + (variation % 5) * 4.0) * tuning / rate
                    val bodyEnv = exp(-t * (17.0 / decayScale))
                    val body = (sin(snarePhase) * 0.52 + sin(snarePhase * 1.91) * 0.12) * bodyEnv
                    val wire = snareNoise.process(noise) * exp(-t * (25.0 / decayScale)) * 0.92
                    val snap = sin(2.0 * PI * 1450.0 * t) * exp(-t * 115.0) * 0.10
                    body + wire + snap
                }

                type.contains("clap") -> {
                    var bursts = 0.0
                    clapBursts.forEach { burst ->
                        val elapsed = t - burst.delay
                        if (elapsed >= 0.0) {
                            val n = burst.random.nextDouble() * 2.0 - 1.0
                            bursts += burst.filter.process(n) * burst.weight * exp(-elapsed * burst.falloff / decayScale)
                        }
                    }
                    val body = sin(2.0 * PI * (205.0 * tuning) * t) * exp(-t * (24.0 / decayScale)) * 0.12
                    val tail = if (t > 0.045) exp(-(t - 0.045) * (15.0 / decayScale)) else 1.0
                    bursts * tail * 2.1 + body
                }

                type.contains("hat") || type.contains("shaker") -> {
                    val open = type.contains("open")
                    val env = exp(-t * ((if (open) 13.0 else 48.0) / decayScale))
                    val metal = sin(2.0 * PI * 3471.0 * t) * 0.30 +
                        sin(2.0 * PI * 5213.0 * t) * 0.25 +
                        sin(2.0 * PI * 6847.0 * t) * 0.18 +
                        sin(2.0 * PI * 8231.0 * t) * 0.10
                    (metal + hatNoise.process(noise) * 0.24) * env
                }

                type.contains("bass") || type.contains("sub") -> {
                    val root = (52.0 + (variation % 5) * 1.5) * tuning
                    bassPhase += 2.0 * PI * root * (1.0 + 0.0025 * sin(2.0 * PI * 4.1 * t)) / rate
                    val attack = min(1.0, t / 0.012)
                    val releaseStart = max(0.0, length / 1000.0 - 0.12)
                    val release = if (t > releaseStart) exp(-(t - releaseStart) * (18.0 / decayScale)) else 1.0
                    (sin(bassPhase) * 0.78 + sin(bassPhase * 2.0) * 0.18 + sin(bassPhase * 3.0) * 0.045) * attack * release
                }

                type.contains("tom") -> {
                    val root = (82.0 + (variation % 5) * 3.0) * tuning
                    val frequency = root + 105.0 * exp(-t * (13.5 / decayScale))
                    tomPhase += 2.0 * PI * frequency / rate
                    (sin(tomPhase) * 0.86 + sin(tomPhase * 2.0) * 0.12) * exp(-t * (7.5 / decayScale))
                }

                type.contains("perc") || type.contains("bongo") -> {
                    val root = (230.0 + (variation % 7) * 8.0) * tuning
                    val frequency = root + 105.0 * exp(-t * (22.0 / decayScale))
                    percPhase1 += 2.0 * PI * frequency / rate
                    percPhase2 += 2.0 * PI * frequency * 1.49 / rate
                    val body = (sin(percPhase1) * 0.72 + sin(percPhase2) * 0.20) * exp(-t * (13.0 / decayScale))
                    body + percNoise.process(noise) * exp(-t * 72.0) * 0.10
                }

                type.contains("pluck") -> {
                    val root = (246.94 + (variation % 5) * 6.0) * tuning
                    pluckPhase1 += 2.0 * PI * root / rate
                    pluckPhase2 += 2.0 * PI * root * 2.01 / rate
                    val env = exp(-t * (5.8 / decayScale))
                    (sin(pluckPhase1) * 0.68 + sin(pluckPhase2) * 0.20 + sin(pluckPhase1 * 3.01) * 0.075) * env
                }

                type.contains("fx") || type.contains("sweep") -> {
                    val frequency = 130.0 * 2.0.pow(3.2 * u) * tuning
                    fxPhase += 2.0 * PI * frequency / rate
                    val sweep = sin(fxPhase) * (0.25 + 0.55 * u)
                    (sweep + fxNoise.process(noise) * 0.035 * u) * (0.90 - 0.25 * u)
                }

                else -> {
                    val root = 220.0 * tuning
                    synthPhase1 += 2.0 * PI * root / rate
                    synthPhase2 += 2.0 * PI * root * 1.25 / rate
                    synthPhase3 += 2.0 * PI * root * 1.5 / rate
                    val attack = min(1.0, t / 0.018)
                    val releaseStart = max(0.0, length / 1000.0 - 0.16)
                    val release = if (t > releaseStart) exp(-(t - releaseStart) * (8.0 / decayScale)) else 1.0
                    (sin(synthPhase1) * 0.46 + sin(synthPhase2) * 0.31 + sin(synthPhase3) * 0.18) * attack * release
                }
            }

            val bright = sample * (0.82 + brightnessScale * 0.18)
            raw[i] = (tanh(bright * driveScale) / tanh(driveScale).coerceAtLeast(0.0001)).toFloat()
        }

        val fadeFrames = min(frames / 3, max(1, rate / 160))
        for (i in 0 until fadeFrames) raw[frames - fadeFrames + i] *= (fadeFrames - i - 1).toFloat() / fadeFrames

        var mean = 0.0
        for (v in raw) mean += v
        mean /= raw.size
        var peak = 0.0
        for (i in raw.indices) {
            raw[i] = (raw[i] - mean.toFloat()).coerceIn(-1f, 1f)
            peak = max(peak, abs(raw[i].toDouble()))
        }
        if (peak < 0.00001) throw IllegalStateException("O sintetizador não produziu áudio útil para $soundType.")
        val gain = (0.86 / peak).coerceAtMost(12.0)
        for (i in raw.indices) raw[i] = (raw[i] * gain.toFloat()).coerceIn(-0.96f, 0.96f)
        return toWav16(raw, rate)
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
