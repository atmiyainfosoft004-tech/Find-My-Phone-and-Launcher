package com.example.findmyphonebyclaplauncher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.domain.detector.ClapDetector
import com.example.findmyphonebyclaplauncher.domain.detector.WhistleDetector
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.utils.Constants
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that reads low-latency 23ms PCM audio frames from microphone
 * and feeds them to [ClapDetector] and [WhistleDetector].
 */
class SoundDetectionService : Service() {

    private val tag = "SoundDetectionService"

    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var recordingJob: Job? = null

    private var audioRecord: AudioRecord? = null

    private lateinit var clapDetector: ClapDetector
    private lateinit var whistleDetector: WhistleDetector

    private var isClapEnabled    = false
    private var isWhistleEnabled = false
    private var warmupUntilTime  = 0L

    private lateinit var userPrefs: UserPreferencesDataSource

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service created")
        userPrefs = UserPreferencesDataSource(applicationContext)
        initDetectors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(tag, "onStartCommand — action=$action")

        when (action) {
            Constants.ACTION_STOP_DETECTION -> {
                stopSelf()
                return START_NOT_STICKY
            }
            Constants.ACTION_START_DETECTION,
            Constants.ACTION_UPDATE_SETTINGS -> {
                val clap    = intent.getBooleanExtra(Constants.EXTRA_CLAP_ENABLED, userPrefs.isClapDetectionEnabled)
                val whistle = intent.getBooleanExtra(Constants.EXTRA_WHISTLE_ENABLED, userPrefs.isWhistleDetectionEnabled)
                applyDetectorSettings(clap, whistle)
            }
            else -> {
                applyDetectorSettings(
                    userPrefs.isClapDetectionEnabled,
                    userPrefs.isWhistleDetectionEnabled
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(tag, "Service destroyed")
        stopRecording()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun initDetectors() {
        clapDetector = ClapDetector(
            onClapDetected = { count ->
                Log.d(tag, "Clap $count detected")
            },
            onThreeClapsDetected = {
                Log.d(tag, "Three claps → triggering Find Phone")
                val flashEnabled = userPrefs.isFlashlightEnabled
                (application as App).findPhoneManager.triggerFindPhone(flashEnabled)
            }
        )

        whistleDetector = WhistleDetector(
            onWhistleDetected = {
                Log.d(tag, "Whistle → triggering Find Phone")
                val flashEnabled = userPrefs.isFlashlightEnabled
                (application as App).findPhoneManager.triggerFindPhone(flashEnabled)
            }
        )
    }

    private fun applyDetectorSettings(clapEnabled: Boolean, whistleEnabled: Boolean) {
        Log.d(tag, "applyDetectorSettings — clap=$clapEnabled, whistle=$whistleEnabled")

        if (!clapEnabled && !whistleEnabled) {
            Log.d(tag, "Both detectors disabled — stopping service")
            stopRecording()
            stopSelf()
            return
        }

        if (!PermissionManager.hasRecordAudioPermission(this)) {
            Log.e(tag, "RECORD_AUDIO permission not granted — cannot start detection")
            stopSelf()
            return
        }

        startForegroundNotification()

        val wasRunning   = recordingJob?.isActive == true
        isClapEnabled    = clapEnabled
        isWhistleEnabled = whistleEnabled

        clapDetector.reset()
        whistleDetector.reset()

        if (!wasRunning) {
            startRecording()
        }
    }

    private fun startRecording() {
        if (recordingJob?.isActive == true) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_CONFIG,
                Constants.AUDIO_FORMAT
            )

            // Low-latency read buffer size: 1024 short samples = ~23.2 ms per audio chunk
            val chunkStepSize = 1024
            val hwBufferSize = (minBufferSize * 2).coerceAtLeast(chunkStepSize * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_CONFIG,
                Constants.AUDIO_FORMAT,
                hwBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord failed to initialise")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            warmupUntilTime = System.currentTimeMillis() + 600L
            Log.d(tag, "AudioRecord started (chunkSize=$chunkStepSize, hwBuffer=$hwBufferSize)")

            recordingJob = serviceScope.launch {
                val buffer = ShortArray(chunkStepSize)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, chunkStepSize) ?: break
                    if (read < 0) break

                    // Ignore hardware mic startup pop for first 600ms
                    if (System.currentTimeMillis() < warmupUntilTime) {
                        continue
                    }

                    if (isClapEnabled)    clapDetector.analyze(buffer, read)
                    if (isWhistleEnabled) whistleDetector.analyze(buffer, read)
                }
                Log.d(tag, "Recording loop exited")
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Microphone permission denied at AudioRecord init: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e(tag, "Error starting AudioRecord: ${e.message}")
            stopSelf()
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(tag, "AudioRecord released")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioRecord: ${e.message}")
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIFICATION_ID_DETECTION,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID_DETECTION, notification)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, FindPhoneActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Constants.CHANNEL_ID_DETECTION)
            .setSmallIcon(R.drawable.ic_notification_phone)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {

        fun startIntent(
            context: Context,
            clapEnabled: Boolean,
            whistleEnabled: Boolean
        ) = Intent(context, SoundDetectionService::class.java).apply {
            action = Constants.ACTION_START_DETECTION
            putExtra(Constants.EXTRA_CLAP_ENABLED, clapEnabled)
            putExtra(Constants.EXTRA_WHISTLE_ENABLED, whistleEnabled)
        }

        fun updateIntent(
            context: Context,
            clapEnabled: Boolean,
            whistleEnabled: Boolean
        ) = Intent(context, SoundDetectionService::class.java).apply {
            action = Constants.ACTION_UPDATE_SETTINGS
            putExtra(Constants.EXTRA_CLAP_ENABLED, clapEnabled)
            putExtra(Constants.EXTRA_WHISTLE_ENABLED, whistleEnabled)
        }

        fun stopIntent(context: Context) =
            Intent(context, SoundDetectionService::class.java).apply {
                action = Constants.ACTION_STOP_DETECTION
            }
    }
}
