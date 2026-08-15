package com.example.findmyphonebyclaplauncher.domain.detector

import android.util.Log
import com.example.findmyphonebyclaplauncher.utils.Constants
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Advanced Pitch-Tracking Whistle Detector with Metallic Noise Discrimination & Sustained Duration Testing.
 *
 * Implements:
 * 1. Pure Fundamental Frequency Analysis (1000 Hz - 3200 Hz).
 * 2. Autocorrelation Pitch Clarity Test (>= 70% spectral energy in fundamental pitch).
 * 3. Dynamic Sensitivity Adjuster (Low, Medium, High).
 * 4. Sustained 300ms Pitch Stability Requirement (+/-100 Hz variance).
 */
class WhistleDetector(
    private val onWhistleDetected: () -> Unit
) {
    private val tag = "WhistleDetector"

    private var consecutiveWhistleFrames = 0
    private var lastDominantFreq = -1.0

    // Dynamic Sensitivity Parameters (Default Medium)
    private var autocorrelationThreshold = 0.75
    private var requiredSustainedFrames = 13 // ~300ms continuous whistle
    private var pitchStabilityTolerance = 100.0
    private var currentSensitivityLevel = "Medium"

    // Whistle search lag range for sample rate 44.1 kHz (f in [1000 Hz, 3200 Hz])
    private val minLag = (Constants.SAMPLE_RATE / Constants.WHISTLE_MAX_FREQUENCY_HZ).toInt().coerceAtLeast(10)
    private val maxLag = (Constants.SAMPLE_RATE / Constants.WHISTLE_MIN_FREQUENCY_HZ).toInt().coerceAtMost(50)

    init {
        Log.d(tag, "Initialized WhistleDetector instance (Sensitivity=$currentSensitivityLevel)")
    }

    fun setSensitivity(sensitivity: String) {
        currentSensitivityLevel = sensitivity
        when (sensitivity.lowercase()) {
            "high" -> {
                autocorrelationThreshold = 0.70
                requiredSustainedFrames = 9 // ~200ms
                pitchStabilityTolerance = 150.0
            }
            "low" -> {
                autocorrelationThreshold = 0.82
                requiredSustainedFrames = 17 // ~400ms
                pitchStabilityTolerance = 80.0
            }
            else -> { // "Medium"
                autocorrelationThreshold = 0.75
                requiredSustainedFrames = 13 // ~300ms
                pitchStabilityTolerance = 100.0
            }
        }
        Log.d(tag, "Sensitivity updated to '$sensitivity' (AutocorrThreshold=$autocorrelationThreshold, SustainedFrames=$requiredSustainedFrames, StabilityTol=$pitchStabilityTolerance)")
    }

    fun analyze(buffer: ShortArray, readSize: Int) {
        if (readSize < 256) return

        val rms = calculateRms(buffer, readSize)

        if (rms < Constants.WHISTLE_AMPLITUDE_THRESHOLD) {
            if (consecutiveWhistleFrames > 0) {
                consecutiveWhistleFrames--
            }
            if (consecutiveWhistleFrames == 0) {
                lastDominantFreq = -1.0
            }
            return
        }

        // 1. Calculate Autocorrelation Peak & Dominant Pitch Frequency
        val (dominantFreq, autocorrelationPeak) = computeAutocorrelationPitch(buffer, readSize)

        // 2. Pitch Clarity Check: Ensures fundamental pitch holds >= 70% spectral energy
        val isPureSinusoid = autocorrelationPeak >= autocorrelationThreshold
        val isWhistleFreq = dominantFreq in Constants.WHISTLE_MIN_FREQUENCY_HZ..Constants.WHISTLE_MAX_FREQUENCY_HZ

        // 3. Pitch Stability Check: Whistle pitch remains steady (within +/-100 Hz variance)
        val isPitchStable = lastDominantFreq < 0 || abs(dominantFreq - lastDominantFreq) <= pitchStabilityTolerance

        if (isWhistleFreq && isPureSinusoid && isPitchStable) {
            consecutiveWhistleFrames++
            lastDominantFreq = dominantFreq
            Log.d(tag, "Whistle frame $consecutiveWhistleFrames/$requiredSustainedFrames (freq=${dominantFreq.toInt()} Hz, clarity=${"%.2f".format(autocorrelationPeak)}, RMS=${rms.toInt()})")

            if (consecutiveWhistleFrames >= requiredSustainedFrames) {
                Log.d(tag, "✓ Valid Whistle pattern confirmed (sustained >=300ms)! Triggering alert.")
                reset()
                onWhistleDetected()
            }
        } else {
            if (consecutiveWhistleFrames > 2) {
                if (!isPureSinusoid) {
                    Log.d(tag, "[REJECTED] Reason: Aperiodic/Multi-Frequency Noise (Purity=${"%.2f".format(autocorrelationPeak)} < $autocorrelationThreshold, Pen Click/Metallic Clink)")
                } else if (!isPitchStable) {
                    Log.d(tag, "[REJECTED] Reason: Transient Metallic Noise (Failed 300ms sustained pitch test, freq shift=${"%.0f".format(abs(dominantFreq - lastDominantFreq))} Hz)")
                }
            }

            if (consecutiveWhistleFrames > 0) {
                consecutiveWhistleFrames -= 2
                if (consecutiveWhistleFrames < 0) consecutiveWhistleFrames = 0
            }
            if (consecutiveWhistleFrames == 0) {
                lastDominantFreq = -1.0
            }
        }
    }

    fun reset() {
        consecutiveWhistleFrames = 0
        lastDominantFreq = -1.0
        Log.d(tag, "WhistleDetector state reset")
    }

    private fun calculateRms(buffer: ShortArray, readSize: Int): Double {
        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / readSize)
    }

    private fun computeAutocorrelationPitch(buffer: ShortArray, readSize: Int): Pair<Double, Double> {
        var r0 = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            r0 += sample * sample
        }
        if (r0 <= 0.0) return Pair(-1.0, 0.0)

        var maxCorr = 0.0
        var bestLag = -1

        for (lag in minLag..maxLag) {
            var corr = 0.0
            val limit = readSize - lag
            for (i in 0 until limit) {
                corr += buffer[i].toDouble() * buffer[i + lag].toDouble()
            }
            val normCorr = corr / r0
            if (normCorr > maxCorr) {
                maxCorr = normCorr
                bestLag = lag
            }
        }

        return if (bestLag > 0 && maxCorr > 0.0) {
            val freq = Constants.SAMPLE_RATE.toDouble() / bestLag.toDouble()
            Pair(freq, maxCorr)
        } else {
            Pair(-1.0, 0.0)
        }
    }
}
