package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen5Binding
import com.example.findmyphonebyclaplauncher.domain.detector.ClapDetector
import com.example.findmyphonebyclaplauncher.domain.detector.WhistleDetector
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingViewModel
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.utils.Constants
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OnboardingScreen5Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen5Binding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    @Volatile
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var executorService: ExecutorService? = null
    private var mediaPlayer: MediaPlayer? = null

    private var clapDetector: ClapDetector? = null
    private var whistleDetector: WhistleDetector? = null
    private var pulseAnimation: ScaleAnimation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTestLater.setOnClickListener {
            stopDetection()
            stopSound()
            (activity as? OnboardingActivity)?.navigateToPage(OnboardingPagerAdapter.PAGE_SCREEN_6)
        }
    }

    override fun onResume() {
        super.onResume()
        startMicAnimation()
        startDetection()
    }

    override fun onPause() {
        super.onPause()
        stopMicAnimation()
        stopDetection()
        stopSound()
    }

    private fun startMicAnimation() {
        if (pulseAnimation != null) return
        pulseAnimation = ScaleAnimation(
            1.0f, 1.08f, 1.0f, 1.08f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.cardMicCircle.startAnimation(pulseAnimation)
    }

    private fun stopMicAnimation() {
        binding.cardMicCircle.clearAnimation()
        pulseAnimation = null
    }

    private fun startDetection() {
        val ctx = context ?: return
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            binding.txtStatusHeader.text = getString(R.string.listening_header)
            return
        }

        clapDetector = ClapDetector(
            onClapDetected = { count ->
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.txtStatusHeader.text = "Clap $count/3 Detected!"
                    }
                }
            },
            onThreeClapsDetected = {
                activity?.runOnUiThread {
                    onSoundDetected("Clap")
                }
            }
        )

        whistleDetector = WhistleDetector(
            onWhistleDetected = {
                activity?.runOnUiThread {
                    onSoundDetected("Whistle")
                }
            }
        )

        isRecording = true
        executorService = Executors.newSingleThreadExecutor()
        executorService?.execute {
            runAudioLoop(ctx)
        }
    }

    private fun runAudioLoop(context: Context) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            Constants.SAMPLE_RATE,
            Constants.CHANNEL_CONFIG,
            Constants.AUDIO_FORMAT
        )
        val bufferSize = minBufferSize * Constants.BUFFER_SIZE_FACTOR
        if (bufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_CONFIG,
                Constants.AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            val buffer = ShortArray(1024)

            while (isRecording && audioRecord != null) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    clapDetector?.analyze(buffer, readSize)
                    whistleDetector?.analyze(buffer, readSize)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioRecord = null
            }
        }
    }

    private fun onSoundDetected(detectedType: String) {
        stopDetection()
        stopMicAnimation()

        if (_binding == null) return
        binding.txtStatusHeader.text = "🎉 $detectedType Detected!"
        binding.txtStatusSub.text = "Playing selected alert sound..."

        playSoundAlert()
    }

    private fun getSoundRawResId(soundId: String): Int? {
        return when (soundId.lowercase()) {
            "airhorn"               -> R.raw.airhorn
            "babylaugh", "baby"     -> R.raw.baby
            "cat"                   -> R.raw.cat
            "dog"                   -> R.raw.dog
            "doorbell", "door_bell" -> R.raw.door_bell
            "train"                 -> R.raw.train
            "hello"                 -> R.raw.hello
            "horn", "car"           -> R.raw.car
            else                    -> null
        }
    }

    private fun playSoundAlert() {
        val ctx = context ?: return
        val prefs = UserPreferencesDataSource(ctx)
        val selectedSound = prefs.selectedAlertSound
        val volFloat = prefs.alertSoundVolume / 100.0f

        try {
            stopSound()
            val rawResId = getSoundRawResId(selectedSound)
            mediaPlayer = if (rawResId != null) {
                MediaPlayer.create(ctx, rawResId)
            } else {
                MediaPlayer.create(ctx, Settings.System.DEFAULT_RINGTONE_URI)
            }

            mediaPlayer?.apply {
                setVolume(volFloat, volFloat)
                isLooping = false
                setOnCompletionListener {
                    activity?.runOnUiThread {
                        if (_binding != null) {
                            binding.txtStatusHeader.text = getString(R.string.listening_header)
                            binding.txtStatusSub.text = getString(R.string.listening_subtitle)
                        }
                    }
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopDetection() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }

        executorService?.shutdownNow()
        executorService = null
        clapDetector = null
        whistleDetector = null
    }

    private fun stopSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMicAnimation()
        stopDetection()
        stopSound()
        _binding = null
    }
}
