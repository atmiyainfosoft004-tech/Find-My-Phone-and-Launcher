package com.example.findmyphonebyclaplauncher.domain.manager

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Robust Flashlight Controller using Camera2 API.
 * Finds the rear camera with hardware flash capability and executes a high-visibility strobe flashing pattern.
 */
class FlashlightManager(private val context: Context) {

    private val tag = "FlashlightManager"
    private var isFlashing = false
    private var isTorchOn = false
    private var flashCameraId: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** Find rear camera ID that has a hardware flash unit. */
    private fun getCameraIdWithFlash(): String? {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
            // Fallback to any camera with flash
            for (id in cameraManager.cameraIdList) {
                val hasFlash = cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) return id
            }
        } catch (e: Exception) {
            Log.e(tag, "Error finding flash camera: ${e.message}")
        }
        return null
    }

    /** Starts high-visibility strobe flashlight flashing (300ms ON / 300ms OFF). */
    fun startFlashing() {
        if (isFlashing) return

        val cameraId = getCameraIdWithFlash()
        if (cameraId == null) {
            Log.w(tag, "No camera with hardware flashlight found on this device")
            return
        }

        flashCameraId = cameraId
        isFlashing = true
        isTorchOn = false
        Log.d(tag, "Starting flashlight strobe on camera $cameraId")

        flashRunnable = object : Runnable {
            override fun run() {
                if (!isFlashing) return
                try {
                    isTorchOn = !isTorchOn
                    cameraManager.setTorchMode(cameraId, isTorchOn)
                } catch (e: Exception) {
                    Log.e(tag, "Error toggling torch: ${e.message}")
                }
                handler.postDelayed(this, 300L) // Flash toggle every 300ms
            }
        }
        handler.post(flashRunnable!!)
    }

    /** Stops flashlight flashing and ensures torch is turned OFF. */
    fun stopFlashing() {
        if (!isFlashing && flashCameraId == null) return
        Log.d(tag, "Stopping flashlight")

        isFlashing = false
        flashRunnable?.let { handler.removeCallbacks(it) }
        flashRunnable = null

        val cameraId = flashCameraId
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: Exception) {
                Log.e(tag, "Error turning off torch: ${e.message}")
            } finally {
                flashCameraId = null
                isTorchOn = false
            }
        }
    }
}
