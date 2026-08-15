package com.example.findmyphonebyclaplauncher.domain.detector

import android.util.Log
import com.example.findmyphonebyclaplauncher.utils.Constants
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Advanced DSP Clap Detector with Mechanical Tap/Click Discrimination & Envelope Profile Analysis.
 *
 * Distinguishes human hand claps from mechanical clicks (pen taps, keys dropping, keyboard typing):
 * 1. Spectral Band Energy Ratio: Human skin claps concentrate energy in 1.5kHz-4.5kHz mid-band.
 *    Hard object impacts generate ultra-high frequency (>6kHz) acoustic resonance.
 * 2. Temporal Envelope Profile: Hard clicks have ultra-short <15ms spikes. Hand claps have a 30-80ms wave envelope.
 * 3. Refractory Lockout (180ms) & Debounce: Prevents room reverberation double counting.
 */
class ClapDetector(
    private val onClapDetected: (clapNumber: Int) -> Unit,
    private val onThreeClapsDetected: () -> Unit
) {
    private val tag = "ClapDetector"

    private val clapTimestamps = mutableListOf<Long>()
    private var lastClapTime = 0L

    // Biquad 1: Mid-Band Filter (1500 Hz - 4500 Hz at 44.1 kHz)
    private var mx1 = 0.0
    private var mx2 = 0.0
    private var my1 = 0.0
    private var my2 = 0.0
    private val mb0 = 0.1717
    private val mb2 = -0.1717
    private val ma1 = -1.5075
    private val ma2 = 0.6566

    // Biquad 2: High-Pass Filter (> 6000 Hz at 44.1 kHz) for mechanical click detection
    private var hx1 = 0.0
    private var hx2 = 0.0
    private var hy1 = 0.0
    private var hy2 = 0.0
    private val hb0 = 0.5399
    private val hb1 = -1.0798
    private val hb2 = 0.5399
    private val ha1 = -0.8554
    private val ha2 = 0.3040

    // Dynamic Noise Floor Tracking
    private var ambientRms = 300.0
    private var previousFrameRms = 300.0

    // Sensitivity Parameters (Default Medium)
    private var minPeakThreshold = 1200
    private var minRmsThreshold = 380.0
    private var minFilteredRms = 180.0
    private var attackRatioThreshold = 2.2
    private var zcrThreshold = 0.05
    private var currentSensitivityLevel = "Medium"

    // Refractory lockout period (180ms)
    private val refractoryPeriodMs = 180L

    // Diagnostic logging timing
    private var lastDiagnosticLogTime = 0L
    private var totalFramesProcessed = 0L

    init {
        Log.d(tag, "Initialized ClapDetector instance (Sensitivity=$currentSensitivityLevel)")
    }

    fun setSensitivity(sensitivity: String) {
        currentSensitivityLevel = sensitivity
        when (sensitivity.lowercase()) {
            "high" -> {
                minPeakThreshold = 6000
                minRmsThreshold = 220.0
                minFilteredRms = 120.0
                attackRatioThreshold = 8.0
                zcrThreshold = 0.04
            }
            "low" -> {
                minPeakThreshold = 18000
                minRmsThreshold = 650.0
                minFilteredRms = 400.0
                attackRatioThreshold = 25.0
                zcrThreshold = 0.07
            }
            else -> { // "Medium"
                minPeakThreshold = 12000
                minRmsThreshold = 380.0
                minFilteredRms = 180.0
                attackRatioThreshold = 15.0
                zcrThreshold = 0.05
            }
        }
        Log.d(tag, "Sensitivity updated to '$sensitivity' (PeakThreshold=$minPeakThreshold, AttackRatioThreshold=$attackRatioThreshold, ZCR=$zcrThreshold)")
    }

    fun analyze(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return

        val now = System.currentTimeMillis()
        totalFramesProcessed++

        // Enforce strict 180ms refractory lockout period after a recorded clap
        if (lastClapTime > 0 && (now - lastClapTime < refractoryPeriodMs)) {
            return
        }

        // Quietly reset sequence counter if gap since last clap exceeds MAX_CLAP_INTERVAL_MS (1500ms)
        if (clapTimestamps.isNotEmpty() && (now - lastClapTime > Constants.MAX_CLAP_INTERVAL_MS)) {
            Log.d(tag, "Gap timer expired (${now - lastClapTime}ms > ${Constants.MAX_CLAP_INTERVAL_MS}ms) — quietly resetting clap count to 0")
            clapTimestamps.clear()
        }

        analyzeAudioChunk(buffer, readSize, now)
    }

    fun reset() {
        clapTimestamps.clear()
        lastClapTime = 0L
        ambientRms = 300.0
        previousFrameRms = 300.0
        mx1 = 0.0; mx2 = 0.0; my1 = 0.0; my2 = 0.0
        hx1 = 0.0; hx2 = 0.0; hy1 = 0.0; hy2 = 0.0
        Log.d(tag, "ClapDetector state reset")
    }

    private fun analyzeAudioChunk(buffer: ShortArray, readSize: Int, now: Long) {
        var rawSumSquares = 0.0
        var midpassSumSquares = 0.0
        var highpassSumSquares = 0.0
        var maxPeak = 0
        var zeroCrossings = 0

        // Sub-chunk energy profile tracking (4 sub-chunks of 256 samples each)
        val subChunkEnergies = DoubleArray(4)
        val subChunkSize = (readSize / 4).coerceAtLeast(1)

        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            val absSample = abs(buffer[i].toInt())
            if (absSample > maxPeak) {
                maxPeak = absSample
            }
            rawSumSquares += sample * sample

            // Sub-chunk energy
            val subIndex = (i / subChunkSize).coerceIn(0, 3)
            subChunkEnergies[subIndex] += sample * sample

            // Biquad 1: Mid-Band Filter (1.5kHz - 4.5kHz)
            val midFiltered = mb0 * sample + mb2 * mx2 - ma1 * my1 - ma2 * my2
            mx2 = mx1; mx1 = sample
            my2 = my1; my1 = midFiltered
            midpassSumSquares += midFiltered * midFiltered

            // Biquad 2: High-Pass Filter (> 6kHz)
            val highFiltered = hb0 * sample + hb1 * hx1 + hb2 * hx2 - ha1 * hy1 - ha2 * hy2
            hx2 = hx1; hx1 = sample
            hy2 = hy1; hy1 = highFiltered
            highpassSumSquares += highFiltered * highFiltered

            if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) ||
                          (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        val rawRms = sqrt(rawSumSquares / readSize)
        val midRms = sqrt(midpassSumSquares / readSize)
        val highRms = sqrt(highpassSumSquares / readSize)
        val zcr = zeroCrossings.toDouble() / readSize.toDouble()

        // High-to-Mid Energy Ratio (Hard mechanical object impacts have very high ratio > 1.25)
        val highMidRatio = highRms / midRms.coerceAtLeast(10.0)

        // Dynamic Noise Floor Tracking
        if (rawRms < ambientRms * 2.0 && maxPeak < 2000) {
            ambientRms = ambientRms * 0.95 + rawRms * 0.05
        }
        ambientRms = ambientRms.coerceIn(100.0, 3000.0)

        // Periodic Diagnostic Log every ~500ms
        if (now - lastDiagnosticLogTime >= 500) {
            lastDiagnosticLogTime = now
            Log.d(tag, "[DIAGNOSTIC] Sensitivity=$currentSensitivityLevel | Peak=$maxPeak | RawRMS=${rawRms.toInt()} | MidRMS=${midRms.toInt()} | HighRMS=${highRms.toInt()} | ZCR=${"%.2f".format(zcr)} | ActiveClaps=${clapTimestamps.size}")
        }

        // 1. Transient Sharp Attack Evaluation
        val attackRatio = rawRms / previousFrameRms.coerceAtLeast(50.0)
        val isSharpAttack = attackRatio >= attackRatioThreshold || rawRms >= (ambientRms * attackRatioThreshold).coerceAtLeast(minRmsThreshold * 1.4)
        previousFrameRms = rawRms

        // 2. Zero-Crossing Rate & High Attack Validation
        val isHighZcr = zcr >= zcrThreshold
        val isZcrValid = isHighZcr || attackRatio >= 10.0 || maxPeak >= 5000 || (maxPeak >= 3000 && attackRatio >= 5.0)

        // 3. Amplitude Threshold Check
        val isAboveThreshold = maxPeak >= minPeakThreshold && (rawRms >= minRmsThreshold || midRms >= minFilteredRms)

        val isPeakCandidate = isAboveThreshold && isSharpAttack && isZcrValid

        if (maxPeak >= (minPeakThreshold * 0.8)) {
            Log.d(tag, "[CANDIDATE PEAK] Peak=$maxPeak, RawRMS=${rawRms.toInt()}, HighMidRatio=${"%.2f".format(highMidRatio)}, AttackRatio=${"%.1f".format(attackRatio)}, ZCR=${"%.2f".format(zcr)} -> Candidate=$isPeakCandidate")
        }

        if (isPeakCandidate) {
            // ── REJECTION FILTER 1: Ultra-High Frequency Mechanical Resonance (Pen / Keys / Keyboard Click) ──
            if (highMidRatio > 1.25 || zcr > 0.38) {
                Log.d(tag, "[REJECTED] Reason: High Frequency Mechanical Noise (Pen/Keyboard/Key Tap detected) | High/Mid Ratio=${"%.2f".format(highMidRatio)}, ZCR=${"%.2f".format(zcr)}")
                return
            }

            // ── REJECTION FILTER 2: Temporal Envelope Decay (< 15ms Ultra-short Click) ──
            val maxSubEnergy = subChunkEnergies.maxOrNull() ?: 0.0
            val totalSubEnergy = subChunkEnergies.sum().coerceAtLeast(1.0)
            val subEnergyRatio = maxSubEnergy / totalSubEnergy
            val isUltraShortImpulse = subEnergyRatio > 0.92 && subChunkEnergies[3] < (maxSubEnergy * 0.03)

            if (isUltraShortImpulse) {
                Log.d(tag, "[REJECTED] Reason: Ultra-short Impulse Width (<15ms, Pen/Keyboard click detected) | SubEnergyRatio=${"%.2f".format(subEnergyRatio)}")
                return
            }

            val timeSinceLastClap = now - lastClapTime

            // Refractory lockout check (< 180ms)
            if (lastClapTime > 0 && timeSinceLastClap < Constants.MIN_CLAP_INTERVAL_MS) {
                Log.d(tag, "Ignored refractory echo peak (interval ${timeSinceLastClap}ms < MIN ${Constants.MIN_CLAP_INTERVAL_MS}ms)")
                return
            }

            // If time since last clap exceeds 1500ms, start fresh sequence
            if (timeSinceLastClap > Constants.MAX_CLAP_INTERVAL_MS) {
                clapTimestamps.clear()
            }

            clapTimestamps.add(now)
            lastClapTime = now
            val currentCount = clapTimestamps.size

            Log.d(tag, "👏 VALID CLAP PASSED! Clap Count: $currentCount/${Constants.CLAP_COUNT_REQUIRED} (Peak=$maxPeak, HighMidRatio=${"%.2f".format(highMidRatio)}, AttackRatio=${"%.1f".format(attackRatio)}, ZCR=${"%.2f".format(zcr)}, Interval=${timeSinceLastClap}ms)")
            onClapDetected(currentCount)

            if (currentCount >= Constants.CLAP_COUNT_REQUIRED) {
                Log.d(tag, "✓ 3-clap pattern recognized within valid time windows! Triggering alert.")
                reset()
                onThreeClapsDetected()
            }
        }
    }
}
