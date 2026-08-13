package com.example.findmyphonebyclaplauncher.domain.detector

import android.util.Log
import com.example.findmyphonebyclaplauncher.utils.Constants
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision whistle detector using Goertzel frequency analysis, pitch-stability tracking, and spectral purity testing.
 * Ignores room music, vocals, and ambient background sound.
 */
class WhistleDetector(
    private val onWhistleDetected: () -> Unit
) {
    private val tag = "WhistleDetector"

    private var consecutiveWhistleFrames = 0
    private var lastDominantFreq = -1.0
    private val requiredWhistleFrames = 16 // 16 frames * ~23ms = ~370ms continuous stable whistle

    private val whistleFrequencies: List<Double> = buildList {
        var f = Constants.WHISTLE_MIN_FREQUENCY_HZ
        val step = 150.0
        while (f <= Constants.WHISTLE_MAX_FREQUENCY_HZ) {
            add(f)
            f += step
        }
    }

    private val lowReferenceFrequencies = listOf(150.0, 300.0, 500.0)

    fun analyze(buffer: ShortArray, readSize: Int) {
        if (readSize < 128) return

        val rms = calculateRms(buffer, readSize)

        if (rms > Constants.WHISTLE_AMPLITUDE_THRESHOLD) {
            val (dominantFreq, maxWhistlePower) = findPeakWhistleFrequency(buffer, readSize)
            val lowRefPower = findMaxLowRefPower(buffer, readSize)

            val isWhistleFreq = dominantFreq in Constants.WHISTLE_MIN_FREQUENCY_HZ..Constants.WHISTLE_MAX_FREQUENCY_HZ
            val isPureTone = maxWhistlePower > (lowRefPower * 2.0) && maxWhistlePower > (rms * rms * 0.10)

            // Whistle pitch stability check: frequency must remain stable (within 200 Hz)
            val isPitchStable = lastDominantFreq < 0 || abs(dominantFreq - lastDominantFreq) <= 250.0

            if (isWhistleFreq && isPureTone && isPitchStable) {
                consecutiveWhistleFrames++
                lastDominantFreq = dominantFreq
                Log.d(tag, "Whistle frame $consecutiveWhistleFrames/$requiredWhistleFrames (freq: $dominantFreq Hz, RMS: $rms)")

                if (consecutiveWhistleFrames >= requiredWhistleFrames) {
                    Log.d(tag, "✓ Valid Whistle confirmed! Triggering Alert!")
                    reset()
                    onWhistleDetected()
                }
            } else {
                if (consecutiveWhistleFrames > 0) {
                    consecutiveWhistleFrames--
                }
                lastDominantFreq = -1.0
            }
        } else {
            reset()
        }
    }

    fun reset() {
        consecutiveWhistleFrames = 0
        lastDominantFreq = -1.0
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

    private fun findPeakWhistleFrequency(buffer: ShortArray, readSize: Int): Pair<Double, Double> {
        var maxPower = 0.0
        var dominant = -1.0

        for (freq in whistleFrequencies) {
            val power = goertzelPower(buffer, readSize, freq)
            if (power > maxPower) {
                maxPower = power
                dominant = freq
            }
        }
        return Pair(dominant, maxPower)
    }

    private fun findMaxLowRefPower(buffer: ShortArray, readSize: Int): Double {
        var maxPower = 0.0
        for (freq in lowReferenceFrequencies) {
            val power = goertzelPower(buffer, readSize, freq)
            if (power > maxPower) {
                maxPower = power
            }
        }
        return maxPower
    }

    private fun goertzelPower(buffer: ShortArray, readSize: Int, targetHz: Double): Double {
        val k = (0.5 + readSize.toDouble() * targetHz / Constants.SAMPLE_RATE).toInt()
        val omega = 2.0 * PI * k / readSize
        val coeff = 2.0 * cos(omega)

        var s0 = 0.0
        var s1 = 0.0
        var s2 = 0.0

        for (i in 0 until readSize) {
            s0 = buffer[i].toDouble() + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }

        val real = s1 - s2 * cos(omega)
        val imag = s2 * sin(omega)
        return real * real + imag * imag
    }
}
