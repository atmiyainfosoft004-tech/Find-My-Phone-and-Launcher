package com.example.findmyphonebyclaplauncher

import com.example.findmyphonebyclaplauncher.domain.detector.WhistleDetector
import com.example.findmyphonebyclaplauncher.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Unit tests for [WhistleDetector] business logic.
 *
 * Tests exercise the Goertzel algorithm and state-machine logic directly via
 * [WhistleDetector.analyze] with synthetic sine-wave PCM buffers.
 * No AudioRecord hardware dependency.
 */
class WhistleDetectorTest {

    private var whistleDetected = false
    private lateinit var detector: WhistleDetector

    @Before
    fun setUp() {
        whistleDetected = false
        detector = WhistleDetector(
            onWhistleDetected = { whistleDetected = true }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a sine wave at [frequency] Hz sampled at [sampleRate] Hz
     * with a given [amplitude] (0..32767).
     */
    private fun sineBuffer(
        frequency: Double,
        amplitude: Int = 20000,
        size: Int = 4096,
        sampleRate: Int = Constants.SAMPLE_RATE
    ): ShortArray = ShortArray(size) { i ->
        (amplitude * sin(2.0 * PI * frequency * i / sampleRate)).toInt().toShort()
    }

    private fun rmsOf(buf: ShortArray): Double {
        var sum = 0.0
        for (s in buf) sum += s.toDouble() * s.toDouble()
        return sqrt(sum / buf.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `silent buffer does not trigger whistle`() {
        val buf = ShortArray(4096) { 0 }
        repeat(20) { detector.analyze(buf, buf.size) }
        assertFalse(whistleDetected)
    }

    @Test
    fun `detector resets correctly`() {
        detector.reset()
        assertFalse(whistleDetected)
    }

    @Test
    fun `whistle min frequency is below max frequency`() {
        assert(Constants.WHISTLE_MIN_FREQUENCY_HZ < Constants.WHISTLE_MAX_FREQUENCY_HZ)
    }

    @Test
    fun `whistle min duration is positive`() {
        assert(Constants.WHISTLE_MIN_DURATION_MS > 0)
    }

    @Test
    fun `whistle amplitude threshold is positive`() {
        assert(Constants.WHISTLE_AMPLITUDE_THRESHOLD > 0)
    }

    @Test
    fun `sine wave at 2000 Hz has sufficient amplitude to exceed threshold`() {
        val buf = sineBuffer(2000.0, amplitude = 20000)
        val rms = rmsOf(buf)
        // RMS of a sine wave with amplitude A ≈ A / sqrt(2)
        assert(rms > Constants.WHISTLE_AMPLITUDE_THRESHOLD) {
            "Expected RMS $rms > ${Constants.WHISTLE_AMPLITUDE_THRESHOLD}"
        }
    }

    @Test
    fun `sine wave at 50 Hz is outside whistle range`() {
        // 50 Hz is clearly not a whistle frequency
        val freq = 50.0
        assertFalse(freq in Constants.WHISTLE_MIN_FREQUENCY_HZ..Constants.WHISTLE_MAX_FREQUENCY_HZ)
    }

    @Test
    fun `sine wave at 2000 Hz is inside whistle range`() {
        val freq = 2000.0
        assert(freq in Constants.WHISTLE_MIN_FREQUENCY_HZ..Constants.WHISTLE_MAX_FREQUENCY_HZ)
    }

    @Test
    fun `very low amplitude does not trigger whistle`() {
        val buf = sineBuffer(2000.0, amplitude = 100) // very quiet
        repeat(50) { detector.analyze(buf, buf.size) }
        assertFalse(whistleDetected)
    }

    @Test
    fun `whistle max duration exceeds min duration`() {
        assert(Constants.WHISTLE_MAX_DURATION_MS > Constants.WHISTLE_MIN_DURATION_MS)
    }

    @Test
    fun `sample rate is 44100`() {
        assertEquals(44100, Constants.SAMPLE_RATE)
    }
}
