package com.nexauren.samplepackcreator

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSynthesizerTest {
    @Test
    fun generatedAudioIsValidPcmWave() {
        val wav = AudioSynthesizer.generate("Kick", durationMs = 500, seed = 1234L)
        assertTrue(wav.size > 44)
        assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
        assertEquals(1, unsignedShort(wav, 20))
        assertEquals(1, unsignedShort(wav, 22))
        assertEquals(44100, littleEndianInt(wav, 24))
        assertEquals(16, unsignedShort(wav, 34))
        assertEquals(wav.size - 44, littleEndianInt(wav, 40))
        val finalPcm = ((wav[wav.lastIndex].toInt() shl 8) or (wav[wav.lastIndex - 1].toInt() and 0xFF)).toShort().toInt()
        assertEquals(0, finalPcm)
    }

    @Test
    fun synthesisIsDeterministicForSameSeed() {
        val first = AudioSynthesizer.generate("Snare", durationMs = 400, variation = 2, seed = 99L)
        val second = AudioSynthesizer.generate("Snare", durationMs = 400, variation = 2, seed = 99L)
        assertArrayEquals(first, second)
    }

    @Test
    fun differentVariationsDoNotProduceIdenticalAudio() {
        val first = AudioSynthesizer.generate("Kick", durationMs = 500, variation = 0, seed = 42L)
        val second = AudioSynthesizer.generate("Kick", durationMs = 500, variation = 1, seed = 42L)
        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun processorCreatesAValidDifferentWave() {
        val original = AudioSynthesizer.generate("Clap", durationMs = 500, seed = 72L)
        val processed = WavProcessor.process(original, 2, listOf(EffectSetting("reverb_room", true, 65f)))
        assertNotNull(processed)
        assertTrue(processed!!.size > 44)
        assertEquals("RIFF", String(processed.copyOfRange(0, 4), Charsets.US_ASCII))
        assertNotEquals(original.toList(), processed.toList())
    }

    @Test
    fun processorLeavesAudioUntouchedWhenNoEffectIsSelected() {
        val original = AudioSynthesizer.generate("Kick", durationMs = 500, seed = 123L)
        val processed = WavProcessor.process(original, 4, emptyList())
        assertNotNull(processed)
        assertArrayEquals(original, processed)
    }

    @Test
    fun synthesisControlsProduceDifferentSoundDesign() {
        val clean = AudioSynthesizer.generate("Synth", durationMs = 600, seed = 2048L)
        val detuned = AudioSynthesizer.generate(
            "Synth",
            durationMs = 600,
            seed = 2048L,
            pitchSemitones = 7f
        )
        val saturated = AudioSynthesizer.generate(
            "Synth",
            durationMs = 600,
            seed = 2048L,
            drive = 3.4f,
            brightness = 1.8f,
            decay = 2.2f
        )
        assertNotEquals(clean.toList(), detuned.toList())
        assertNotEquals(clean.toList(), saturated.toList())
    }

    @Test
    fun everyBuiltInSoundPresetProducesValidWav() {
        AudioSynthesizer.soundTypes.forEach { type ->
            val wav = AudioSynthesizer.generate(type, durationMs = 120, seed = 77L)
            assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
            assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
            assertTrue(wav.size > 44)
        }
    }



    @Test
    fun synthesizedPresetsHaveUsefulOutputLevelAndMusicalBody() {
        listOf("Kick", "Snare", "Clap", "Hi-Hat", "Bass", "Percussion", "Synth", "FX", "Pluck", "Tom").forEach { type ->
            val wav = AudioSynthesizer.generate(type, durationMs = 700, seed = 2026L)
            val pcm = FloatArray((wav.size - 44) / 2) { index ->
                val offset = 44 + index * 2
                val value = ((wav[offset].toInt() and 0xff) or (wav[offset + 1].toInt() shl 8)).toShort().toInt()
                value / 32768f
            }
            val peak = pcm.maxOf { kotlin.math.abs(it) }
            val rms = kotlin.math.sqrt(pcm.map { it * it }.average()).toFloat()
            assertTrue("$type peak too quiet: $peak", peak > 0.65f)
            assertTrue("$type RMS too quiet: $rms", rms > 0.015f)
        }
    }

    @Test
    fun allAvailableEffectsAreIndividuallyConfigurableAndRenderAudio() {
        val original = AudioSynthesizer.generate("Kick", durationMs = 180, seed = 174L)
        assertTrue(EffectCatalog.all.size > 50)
        EffectCatalog.all.forEachIndexed { index, effect ->
            val rendered = WavProcessor.process(original, index, listOf(EffectSetting(effect.id, enabled = true, intensity = 65f)))
            assertNotNull("Effect failed: ${effect.name}", rendered)
            assertTrue("Effect did not produce audio: ${effect.name}", rendered!!.size > 44)
            assertEquals("RIFF", String(rendered.copyOfRange(0, 4), Charsets.US_ASCII))
        }
    }


    @Test
    fun reverseAndReverseGateChainKeepsUsefulClapLevel() {
        val source = AudioSynthesizer.generate("Clap", durationMs = 250, seed = 9081L)
        val processed = WavProcessor.process(
            source,
            2,
            listOf(
                EffectSetting("reverse", enabled = true, intensity = 55f),
                EffectSetting("reverse_gate", enabled = true, intensity = 55f)
            )
        )
        assertNotNull(processed)
        val originalPeak = pcmPeak(source)
        val processedPeak = pcmPeak(processed!!)
        assertTrue("Reverse chain crushed the sample: original=$originalPeak processed=$processedPeak", processedPeak > originalPeak * 0.55f)
        assertTrue("Reverse chain clipped the sample: $processedPeak", processedPeak <= 0.98f)
    }

    private fun pcmPeak(wav: ByteArray): Float {
        var peak = 0f
        var offset = 44
        while (offset + 1 < wav.size) {
            val value = ((wav[offset].toInt() and 0xff) or (wav[offset + 1].toInt() shl 8)).toShort().toInt()
            peak = maxOf(peak, kotlin.math.abs(value / 32768f))
            offset += 2
        }
        return peak
    }

    private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            (bytes[offset + 3].toInt() shl 24)
}
