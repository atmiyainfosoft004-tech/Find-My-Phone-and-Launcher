package com.example.findmyphonebyclaplauncher.ui.settings

import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityAlertSoundBinding
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.SoundItem
import com.example.findmyphonebyclaplauncher.ui.onboarding.adapter.SoundPickerAdapter

class AlertSoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertSoundBinding
    private lateinit var prefs: UserPreferencesDataSource

    private var previewPlayer: MediaPlayer? = null
    private var selectedSoundId = "whistle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferencesDataSource(this)
        selectedSoundId = prefs.selectedAlertSound

        setupWindowInsets()
        setupVolumeSlider()
        setupRecyclerView()
        setupListeners()

        // Auto-play preview of selected sound on screen load (matches OnboardingScreen4Fragment)
        playPreview()
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(
                binding.root.paddingLeft,
                systemBars.top,
                binding.root.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }

    private fun setupVolumeSlider() {
        val savedVolume = prefs.alertSoundVolume
        binding.seekBarVolume.max = 100
        binding.seekBarVolume.progress = savedVolume
        binding.txtVolumePercent.text = "${savedVolume}%"

        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtVolumePercent.text = "${progress}%"
                // Transient preview volume update (do NOT save to prefs here)
                val volFloat = progress / 100.0f
                previewPlayer?.setVolume(volFloat, volFloat)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
            // Transient selection update (do NOT save to prefs here)
            playPreview()
        }
        binding.rvSounds.layoutManager = GridLayoutManager(this, 3)
        binding.rvSounds.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            // Cancel / Back: do NOT save settings
            stopPreview()
            finish()
        }

        binding.btnSave.setOnClickListener {
            // Save settings only when btnSave is clicked
            saveSettings()
            stopPreview()
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Cancel / Back: do NOT save settings
                stopPreview()
                finish()
            }
        })
    }

    private fun saveSettings() {
        prefs.selectedAlertSound = selectedSoundId
        prefs.alertSoundVolume = binding.seekBarVolume.progress
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
        try {
            stopPreview()
            val rawResId = getSoundRawResId(selectedSoundId)
            previewPlayer = if (rawResId != null) {
                MediaPlayer.create(this, rawResId)
            } else {
                MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI)
            }

            val volFloat = binding.seekBarVolume.progress / 100.0f
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
}
