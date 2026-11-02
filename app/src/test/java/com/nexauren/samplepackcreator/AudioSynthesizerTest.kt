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
        val processed = WavProcessor.process(original, 2)
        assertNotNull(processed)
        assertTrue(processed!!.size > 44)
        assertEquals("RIFF", String(processed.copyOfRange(0, 4), Charsets.US_ASCII))
        assertNotEquals(original.toList(), processed.toList())
    }

    private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            (bytes[offset + 3].toInt() shl 24)
}
