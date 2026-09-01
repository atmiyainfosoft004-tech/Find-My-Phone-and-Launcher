package com.example.findmyphonebyclaplauncher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.findmyphonebyclaplauncher.App
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.domain.detector.ClapDetector
import com.example.findmyphonebyclaplauncher.domain.detector.WhistleDetector
import com.example.findmyphonebyclaplauncher.ui.alert.AlertActivity
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
 * Persistent Foreground Service that reads low-latency 23ms PCM audio frames from microphone.
 * Feeds audio frames to [ClapDetector] and [WhistleDetector].
 * Holds CPU PARTIAL_WAKE_LOCK to guarantee screen-off continuous detection.
 */
class SoundDetectionService : Service() {

    private val tag = "SoundDetectionService"

    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var recordingJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var wakeLock: PowerManager.WakeLock? = null

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
        releaseWakeLock()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun initDetectors() {
        clapDetector = ClapDetector(
            onClapDetected = { count ->
                Log.d(tag, "Clap $count detected")
            },
            onThreeClapsDetected = {
                val findPhoneManager = (application as? App)?.findPhoneManager
                if (findPhoneManager?.isAlertActive() != true && !AlertActivity.isAlertActivityVisible) {
                    Log.d(tag, "Three claps → triggering Find Phone")
                    val flashEnabled = userPrefs.isFlashlightEnabled
                    findPhoneManager?.triggerFindPhone(flashEnabled)
                } else {
                    Log.d(tag, "Three claps detected but alert is already active — ignoring")
                }
            }
        )

        whistleDetector = WhistleDetector(
            onWhistleDetected = {
                val findPhoneManager = (application as? App)?.findPhoneManager
                if (findPhoneManager?.isAlertActive() != true && !AlertActivity.isAlertActivityVisible) {
                    Log.d(tag, "Whistle → triggering Find Phone")
                    val flashEnabled = userPrefs.isFlashlightEnabled
                    findPhoneManager?.triggerFindPhone(flashEnabled)
                } else {
                    Log.d(tag, "Whistle detected but alert is already active — ignoring")
                }
            }
        )

        updateDetectorSensitivity()
    }

    private fun updateDetectorSensitivity() {
        val currentSensitivity = userPrefs.alertSensitivity
        clapDetector.setSensitivity(currentSensitivity)
        whistleDetector.setSensitivity(currentSensitivity)
        Log.d(tag, "Sensitivity updated to: $currentSensitivity")
    }

    private fun applyDetectorSettings(clapEnabled: Boolean, whistleEnabled: Boolean) {
        Log.d(tag, "applyDetectorSettings — clap=$clapEnabled, whistle=$whistleEnabled, sensitivity=${userPrefs.alertSensitivity}")

        if (!clapEnabled && !whistleEnabled) {
            Log.d(tag, "Both detectors disabled — stopping service")
            stopRecording()
            releaseWakeLock()
            stopSelf()
            return
        }

        if (!PermissionManager.hasRecordAudioPermission(this)) {
            Log.e(tag, "RECORD_AUDIO permission not granted — cannot start detection")
            stopSelf()
            return
        }

        startForegroundNotification()
        acquireWakeLock()
        updateDetectorSensitivity()

        val wasRunning   = recordingJob?.isActive == true
        isClapEnabled    = clapEnabled
        isWhistleEnabled = whistleEnabled

        clapDetector.reset()
        whistleDetector.reset()

        if (!wasRunning) {
            startRecording()
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null || wakeLock?.isHeld != true) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "FindMyPhoneByClapLauncher::AudioDetectionWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Log.d(tag, "PowerManager PARTIAL_WAKE_LOCK acquired for screen-off detection")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire PARTIAL_WAKE_LOCK: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(tag, "PowerManager PARTIAL_WAKE_LOCK released")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to release PARTIAL_WAKE_LOCK: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    private fun startRecording() {
        if (recordingJob?.isActive == true) {
            Log.d(tag, "startRecording ignored — recordingJob already active")
            return
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_CONFIG,
                Constants.AUDIO_FORMAT
            )

            val chunkStepSize = 1024
            val hwBufferSize = (minBufferSize * 2).coerceAtLeast(chunkStepSize * 4)

            Log.d(tag, "Initializing AudioRecord — SampleRate=${Constants.SAMPLE_RATE}, ChannelConfig=${Constants.CHANNEL_CONFIG}, AudioFormat=${Constants.AUDIO_FORMAT}, MinBuffer=$minBufferSize, HwBuffer=$hwBufferSize")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_CONFIG,
                Constants.AUDIO_FORMAT,
                hwBufferSize
            )

            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord failed to initialize! State=$state, SampleRate=${Constants.SAMPLE_RATE}, HwBuffer=$hwBufferSize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            initAudioEffects(audioRecord?.audioSessionId ?: 0)

            audioRecord?.startRecording()
            warmupUntilTime = System.currentTimeMillis() + 600L
            Log.d(tag, "AudioRecord successfully started (chunkSize=$chunkStepSize, hwBuffer=$hwBufferSize) | ClapEnabled=$isClapEnabled, WhistleEnabled=$isWhistleEnabled")

            recordingJob = serviceScope.launch {
                val buffer = ShortArray(chunkStepSize)
                var loopCount = 0L
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, chunkStepSize) ?: break
                    if (read < 0) {
                        Log.e(tag, "AudioRecord.read returned error code: $read")
                        break
                    }

                    val findPhoneManager = (application as? App)?.findPhoneManager
                    val isAlertRunning = findPhoneManager?.isAlertActive() == true || AlertActivity.isAlertActivityVisible
                    if (isAlertRunning) {
                        clapDetector.reset()
                        whistleDetector.reset()
                        warmupUntilTime = System.currentTimeMillis() + 600L
                        continue
                    }

                    if (System.currentTimeMillis() < warmupUntilTime) {
                        continue
                    }

                    loopCount++
                    if (loopCount % 200L == 0L) {
                        Log.d(tag, "Audio recording loop active — processed $loopCount frames | ClapEnabled=$isClapEnabled, WhistleEnabled=$isWhistleEnabled, Sensitivity=${userPrefs.alertSensitivity}")
                    }

                    if (isClapEnabled)    clapDetector.analyze(buffer, read)
                    if (isWhistleEnabled) whistleDetector.analyze(buffer, read)
                }
                Log.d(tag, "Recording loop exited after $loopCount frames")
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Microphone permission denied at AudioRecord init: ${e.message}", e)
            stopSelf()
        } catch (e: Exception) {
            Log.e(tag, "Error starting AudioRecord: ${e.message}", e)
            stopSelf()
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return
        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
                Log.d(tag, "Hardware NoiseSuppressor enabled for session $sessionId")
            }
            if (AutomaticGainControl.isAvailable()) {
                automaticGainControl = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                }
                Log.d(tag, "Hardware AutomaticGainControl enabled for session $sessionId")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize hardware audio effects: ${e.message}")
        }
    }

    private fun releaseAudioEffects() {
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            automaticGainControl?.release()
            automaticGainControl = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audio effects: ${e.message}")
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        releaseAudioEffects()
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
