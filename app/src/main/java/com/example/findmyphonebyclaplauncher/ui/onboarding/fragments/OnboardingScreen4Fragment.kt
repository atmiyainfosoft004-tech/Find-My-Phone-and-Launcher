package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen4Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingViewModel
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.OnboardingPagerAdapter
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.SoundItem
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.SoundPickerAdapter

class OnboardingScreen4Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen4Binding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    private var previewPlayer: MediaPlayer? = null
    private var selectedSoundId = "whistle"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        val prefs = UserPreferencesDataSource(ctx)

        selectedSoundId = prefs.selectedAlertSound

        setupRecyclerView()
        setupVolumeSlider(prefs)
        setupButtons()

        // Auto-play preview of selected sound on screen load
        playPreview()
    }

    private fun getSoundList(): List<SoundItem> {
        return listOf(
            SoundItem("whistle",    getString(R.string.your_ringtone), R.drawable.ic_sound_ringtone),
            SoundItem("airhorn",    getString(R.string.air_horn),      R.drawable.ic_sound_airhorn),
            SoundItem("babylaugh",  getString(R.string.baby_laugh),    R.drawable.ic_sound_baby),
            SoundItem("cat",        getString(R.string.cat),           R.drawable.ic_sound_cat),
            SoundItem("dog",        getString(R.string.dog),           R.drawable.ic_sound_dog),
            SoundItem("doorbell",   getString(R.string.door_bell),     R.drawable.ic_sound_doorbell),
            SoundItem("train",      getString(R.string.train),         R.drawable.ic_sound_train),
            SoundItem("hello",      getString(R.string.hello),         R.drawable.ic_sound_hello),
            SoundItem("horn",       getString(R.string.horn),          R.drawable.ic_sound_horn)
        )
    }

    private fun setupRecyclerView() {
        val adapter = SoundPickerAdapter(getSoundList(), selectedSoundId) { item ->
            selectedSoundId = item.id
            saveSettings()
            playPreview()
        }
        binding.rvSounds.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvSounds.adapter = adapter
    }

    private fun setupVolumeSlider(prefs: UserPreferencesDataSource) {
        val savedVolume = prefs.alertSoundVolume
        binding.sliderVolume.max = 100
        binding.sliderVolume.progress = savedVolume
        binding.txtVolumeValue.text = "${savedVolume}%"

        binding.sliderVolume.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtVolumeValue.text = "${progress}%"
                val ctx = context ?: return
                val userPrefs = UserPreferencesDataSource(ctx)
                userPrefs.alertSoundVolume = progress

                // Adjust player volume in real-time
                val volFloat = progress / 100.0f
                previewPlayer?.setVolume(volFloat, volFloat)

                if (fromUser) {
                    applySystemVolume(ctx, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupButtons() {
        binding.txtSkip.paintFlags = binding.txtSkip.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        binding.btnTestItNow.setOnClickListener {
            saveSettings()
            stopPreview()
            (activity as? OnboardingActivity)?.navigateToPage(OnboardingPagerAdapter.PAGE_SCREEN_5)
        }

        binding.txtSkip.setOnClickListener {
            saveSettings()
            val ctx = context ?: return@setOnClickListener
            val prefs = UserPreferencesDataSource(ctx)
            prefs.isOnboardingSkipped = true
            stopPreview()
            (activity as? OnboardingActivity)?.navigateToPage(OnboardingPagerAdapter.PAGE_SCREEN_6)
        }
    }

    private fun saveSettings() {
        val ctx = context ?: return
        val prefs = UserPreferencesDataSource(ctx)
        prefs.selectedAlertSound = selectedSoundId
        prefs.alertSoundVolume = binding.sliderVolume.progress
    }

    private fun applySystemVolume(context: Context, volumePercent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val newAlarmVol = (maxAlarm * (volumePercent / 100.0f)).toInt().coerceIn(1, maxAlarm)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newAlarmVol, 0)

            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newMusicVol = (maxMusic * (volumePercent / 100.0f)).toInt().coerceIn(1, maxMusic)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newMusicVol, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun playPreview() {
        val ctx = context ?: return
        try {
            stopPreview()
            val rawResId = getSoundRawResId(selectedSoundId)
            previewPlayer = if (rawResId != null) {
                MediaPlayer.create(ctx, rawResId)
            } else {
                MediaPlayer.create(ctx, Settings.System.DEFAULT_RINGTONE_URI)
            }

            val volFloat = binding.sliderVolume.progress / 100.0f
            previewPlayer?.apply {
                setVolume(volFloat, volFloat)
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            previewPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        stopPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPreview()
        _binding = null
    }
}
