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

    private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            (bytes[offset + 3].toInt() shl 24)
}
