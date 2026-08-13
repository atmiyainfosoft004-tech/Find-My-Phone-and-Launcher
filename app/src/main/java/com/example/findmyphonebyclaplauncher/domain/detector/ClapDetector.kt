package com.example.findmyphonebyclaplauncher.domain.detector

import android.util.Log
import com.example.findmyphonebyclaplauncher.utils.Constants
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-precision impulse clap detector.
 *
 * Verifies sharp impulse rise time, falling edge, high-frequency zero-crossing rate (ZCR),
 * and 3-clap sequence timing. Rejects room music, speech, and bass drum beats.
 */
class ClapDetector(
    private val onClapDetected: (clapNumber: Int) -> Unit,
    private val onThreeClapsDetected: () -> Unit
) {
    private val tag = "ClapDetector"

    private val clapTimestamps = mutableListOf<Long>()
    private var isInClapPeak = false
    private var clapPeakStartTime = 0L
    private var lastClapEndTime = 0L

    fun analyze(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return

        val rms = calculateRms(buffer, readSize)
        val zcr = calculateZcr(buffer, readSize)
        val now = System.currentTimeMillis()

        analyzeForClap(rms, zcr, now)
        pruneExpiredClaps(now)
    }

    fun reset() {
        clapTimestamps.clear()
        isInClapPeak = false
        clapPeakStartTime = 0L
        lastClapEndTime = 0L
        Log.d(tag, "State reset")
    }

    private fun calculateRms(buffer: ShortArray, readSize: Int): Double {
        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / readSize)
    }

    /** Calculates Zero-Crossing Rate (ZCR) — claps have high ZCR due to high-frequency snap. */
    private fun calculateZcr(buffer: ShortArray, readSize: Int): Double {
        var count = 0
        for (i in 1 until readSize) {
            if ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0)) {
                count++
            }
        }
        return count.toDouble() / readSize.toDouble()
    }

    private fun analyzeForClap(rms: Double, zcr: Double, now: Long) {
        val isAboveThreshold = rms > Constants.CLAP_DETECTION_THRESHOLD
        val isBelowSilence   = rms < Constants.CLAP_SILENCE_THRESHOLD
        val isHighZcr        = zcr > 0.10 // Reject low-frequency bass drum beats in music

        // ── 1. Rising edge: sharp high-frequency clap impulse ──────────
        if (isAboveThreshold && isHighZcr && !isInClapPeak) {
            val timeSinceLastClap = now - lastClapEndTime
            if (timeSinceLastClap >= Constants.MIN_CLAP_INTERVAL_MS) {
                isInClapPeak = true
                clapPeakStartTime = now
                Log.d(tag, "Clap peak started (RMS=$rms, ZCR=$zcr)")
            }
        }
        // ── 2. Falling edge: impulse drops back down ───────────────────
        else if (isBelowSilence && isInClapPeak) {
            val duration = now - clapPeakStartTime
            lastClapEndTime = now
            isInClapPeak = false

            if (duration in Constants.CLAP_MIN_DURATION_MS..Constants.CLAP_MAX_DURATION_MS) {
                recordClap(now)
            } else {
                Log.d(tag, "Rejected peak duration=${duration}ms (outside clap range)")
            }
        }
        // ── 3. Timeout safeguard: sustained sound (music/speech) ───────
        else if (isInClapPeak) {
            val duration = now - clapPeakStartTime
            if (duration > Constants.CLAP_MAX_DURATION_MS) {
                isInClapPeak = false
                Log.d(tag, "Peak timed out after ${duration}ms (sustained noise/music, not a clap)")
            }
        }
    }

    private fun recordClap(now: Long) {
        if (clapTimestamps.isNotEmpty()) {
            val gap = now - clapTimestamps.last()
            if (gap > Constants.MAX_CLAP_INTERVAL_MS) {
                Log.d(tag, "Sequence reset — gap=${gap}ms exceeded MAX_CLAP_INTERVAL")
                clapTimestamps.clear()
            }
        }

        clapTimestamps.add(now)
        val count = clapTimestamps.size
        Log.d(tag, "👏 Clap $count/3 detected! Timestamps in window: $count")
        onClapDetected(count)

        if (count >= Constants.CLAP_COUNT_REQUIRED) {
            Log.d(tag, "✓ Three-clap sequence complete → Triggering Alert!")
            clapTimestamps.clear()
            onThreeClapsDetected()
        }
    }

    private fun pruneExpiredClaps(now: Long) {
        if (clapTimestamps.isEmpty()) return
        val cutoff = now - Constants.MAX_CLAP_SEQUENCE_DURATION_MS
        val removed = clapTimestamps.removeAll { it < cutoff }
        if (removed) Log.d(tag, "Pruned expired claps; remaining count: ${clapTimestamps.size}")
    }
}
