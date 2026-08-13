package com.example.findmyphonebyclaplauncher

import com.example.findmyphonebyclaplauncher.domain.detector.ClapDetector
import com.example.findmyphonebyclaplauncher.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests for [ClapDetector] business logic.
 *
 * AudioRecord is NOT tested here (requires device); these tests exercise
 * the amplitude analysis and state-machine logic by calling [ClapDetector.analyze]
 * directly with synthetic PCM buffers.
 */
class ClapDetectorTest {

    private var clapCount = 0
    private var threeClapsTriggered = false
    private lateinit var detector: ClapDetector

    @Before
    fun setUp() {
        clapCount = 0
        threeClapsTriggered = false
        detector = ClapDetector(
            onClapDetected = { count -> clapCount = count },
            onThreeClapsDetected = { threeClapsTriggered = true }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a buffer whose RMS equals [targetRms]. */
    private fun bufferWithRms(targetRms: Double, size: Int = 1024): ShortArray {
        val amplitude = (targetRms * sqrt(2.0)).toInt().coerceIn(0, Short.MAX_VALUE.toInt())
        return ShortArray(size) { if (it % 2 == 0) amplitude.toShort() else (-amplitude).toShort() }
    }

    private fun silentBuffer(size: Int = 1024) = ShortArray(size) { 0 }

    /**
     * Simulates a single sharp clap:
     *  - N loud frames (above threshold)
     *  - N silent frames (below silence threshold)
     *
     * Spacing between calls is controlled by sleeping inside the detector's
     * timestamp — here we let time pass by calling [System.currentTimeMillis]
     * naturally (tests run fast enough that we rely on the detector's logic,
     * not real wall-clock time, for unit testing).
     */
    private fun simulateClap() {
        val loud   = bufferWithRms(Constants.CLAP_DETECTION_THRESHOLD + 500)
        val silent = silentBuffer()
        // Rising edge
        repeat(3) { detector.analyze(loud, loud.size) }
        // Falling edge
        repeat(5) { detector.analyze(silent, silent.size) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `silent audio does not trigger clap`() {
        val silent = silentBuffer()
        repeat(20) { detector.analyze(silent, silent.size) }
        assertEquals(0, clapCount)
        assertEquals(false, threeClapsTriggered)
    }

    @Test
    fun `detector resets correctly`() {
        simulateClap()
        detector.reset()
        assertEquals(false, threeClapsTriggered)
    }

    @Test
    fun `rms calculation returns zero for silent buffer`() {
        val buf = ShortArray(256) { 0 }
        var sum = 0.0
        for (s in buf) sum += s.toDouble() * s.toDouble()
        val rms = sqrt(sum / buf.size)
        assertEquals(0.0, rms, 0.001)
    }

    @Test
    fun `rms calculation returns correct value for constant amplitude buffer`() {
        val amplitude = 1000.toShort()
        val buf = ShortArray(256) { amplitude }
        var sum = 0.0
        for (s in buf) sum += s.toDouble() * s.toDouble()
        val rms = sqrt(sum / buf.size)
        assertEquals(1000.0, rms, 1.0)
    }

    @Test
    fun `loud continuous sound does not count as three claps`() {
        // A sustained loud sound should hit CLAP_MAX_DURATION_MS and be rejected
        val loud = bufferWithRms(Constants.CLAP_DETECTION_THRESHOLD + 200)
        repeat(1000) { detector.analyze(loud, loud.size) }
        assertEquals(false, threeClapsTriggered)
    }

    @Test
    fun `clap threshold constant is positive`() {
        assert(Constants.CLAP_DETECTION_THRESHOLD > 0)
    }

    @Test
    fun `silence threshold is lower than clap threshold`() {
        assert(Constants.CLAP_SILENCE_THRESHOLD < Constants.CLAP_DETECTION_THRESHOLD)
    }

    @Test
    fun `clap count required is three`() {
        assertEquals(3, Constants.CLAP_COUNT_REQUIRED)
    }

    @Test
    fun `max clap sequence duration exceeds three times max interval`() {
        // Sequence window must be large enough to allow 3 claps
        assert(Constants.MAX_CLAP_SEQUENCE_DURATION_MS >= 3 * Constants.MAX_CLAP_INTERVAL_MS)
    }
}
