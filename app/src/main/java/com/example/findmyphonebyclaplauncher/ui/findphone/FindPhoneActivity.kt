package com.example.findmyphonebyclaplauncher.ui.findphone

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.ActivityFindPhoneBinding
import com.example.findmyphonebyclaplauncher.ui.settings.SettingsActivity
import com.example.findmyphonebyclaplauncher.utils.BatteryOptimizationManager
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import com.google.android.material.snackbar.Snackbar

class FindPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindPhoneBinding
    private val viewModel: FindPhoneViewModel by viewModels()

    private var previewMediaPlayer: MediaPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        handlePermissionResult(micGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutHeader.setPadding(
                binding.layoutHeader.paddingLeft,
                systemBars.top + resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                binding.layoutHeader.paddingRight,
                binding.layoutHeader.paddingBottom
            )
            binding.scrollContent.setPadding(
                binding.scrollContent.paddingLeft,
                binding.scrollContent.paddingTop,
                binding.scrollContent.paddingRight,
                systemBars.bottom + resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        checkAndRequestPermissionsIfNeeded()
    }

    private fun checkAndRequestPermissionsIfNeeded() {
        if (!PermissionManager.hasRecordAudioPermission(this)) {
            requestDetectionPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        stopSoundPreview()
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.swMaster.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !PermissionManager.hasRecordAudioPermission(this)) {
                binding.swMaster.isChecked = false
                requestDetectionPermissions()
                return@setOnCheckedChangeListener
            }
            viewModel.setMasterDetection(this, isChecked)
        }

        binding.swClap.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !PermissionManager.hasRecordAudioPermission(this)) {
                binding.swClap.isChecked = false
                requestDetectionPermissions()
                return@setOnCheckedChangeListener
            }
            viewModel.setClapDetection(this, isChecked)
        }

        binding.swWhistle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !PermissionManager.hasRecordAudioPermission(this)) {
                binding.swWhistle.isChecked = false
                requestDetectionPermissions()
                return@setOnCheckedChangeListener
            }
            viewModel.setWhistleDetection(this, isChecked)
        }

        binding.swSoundAlert.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSoundAlertEnabled(isChecked)
        }

        binding.swFlashlightAlert.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFlashlightEnabled(isChecked)
        }

        binding.swVibrationAlert.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVibrationEnabled(isChecked)
        }

        binding.rgSounds.setOnCheckedChangeListener { _, checkedId ->
            val soundKey = when (checkedId) {
                R.id.rbSoundWhistle   -> "whistle"
                R.id.rbSoundMelody    -> "melody"
                R.id.rbSoundAnimal    -> "animal"
                R.id.rbSoundElectronic -> "electronic"
                else                  -> "whistle"
            }
            viewModel.setSelectedAlertSound(soundKey)
        }

        binding.btnPreviewSound.setOnClickListener {
            toggleSoundPreview()
        }

        binding.btnDuration10.setOnClickListener  { viewModel.setSelectedAlertDuration(10) }
        binding.btnDuration30.setOnClickListener  { viewModel.setSelectedAlertDuration(30) }
        binding.btnDuration60.setOnClickListener  { viewModel.setSelectedAlertDuration(60) }
        binding.btnDuration120.setOnClickListener { viewModel.setSelectedAlertDuration(120) }

        binding.btnOpenBatteryOpt.setOnClickListener {
            BatteryOptimizationManager.requestIgnoreBatteryOptimizations(this)
        }
    }

    private fun observeViewModel() {
        viewModel.isClapEnabled.observe(this) { enabled ->
            if (binding.swClap.isChecked != enabled) {
                binding.swClap.isChecked = enabled
            }
            updateMasterStatusText()
        }

        viewModel.isWhistleEnabled.observe(this) { enabled ->
            if (binding.swWhistle.isChecked != enabled) {
                binding.swWhistle.isChecked = enabled
            }
            updateMasterStatusText()
        }

        viewModel.isServiceRunning.observe(this) { running ->
            if (binding.swMaster.isChecked != running) {
                binding.swMaster.isChecked = running
            }
            updateMasterStatusText()
        }

        viewModel.isSoundAlertEnabled.observe(this) { enabled ->
            binding.swSoundAlert.isChecked = enabled
        }

        viewModel.isFlashlightEnabled.observe(this) { enabled ->
            binding.swFlashlightAlert.isChecked = enabled
        }

        viewModel.isVibrationEnabled.observe(this) { enabled ->
            binding.swVibrationAlert.isChecked = enabled
        }

        viewModel.selectedAlertSound.observe(this) { sound ->
            val checkedId = when (sound) {
                "whistle"    -> R.id.rbSoundWhistle
                "melody"     -> R.id.rbSoundMelody
                "animal"     -> R.id.rbSoundAnimal
                "electronic" -> R.id.rbSoundElectronic
                else         -> R.id.rbSoundWhistle
            }
            if (binding.rgSounds.checkedRadioButtonId != checkedId) {
                binding.rgSounds.check(checkedId)
            }
        }

        viewModel.selectedAlertDuration.observe(this) { duration ->
            highlightDurationButton(duration)
        }
    }

    private fun updateMasterStatusText() {
        val active = (viewModel.isClapEnabled.value == true) || (viewModel.isWhistleEnabled.value == true)
        if (active) {
            binding.txtMasterStatus.text = getString(R.string.service_status_active)
            binding.txtMasterStatus.setTextColor(getColor(R.color.color_success))
        } else {
            binding.txtMasterStatus.text = getString(R.string.service_status_off)
            binding.txtMasterStatus.setTextColor(getColor(R.color.color_text_secondary))
        }
    }

    private fun highlightDurationButton(selected: Int) {
        val activeColor = getColor(R.color.color_primary)
        val inactiveColor = getColor(R.color.color_card)

        binding.btnDuration10.setBackgroundColor(if (selected == 10)  activeColor else inactiveColor)
        binding.btnDuration30.setBackgroundColor(if (selected == 30)  activeColor else inactiveColor)
        binding.btnDuration60.setBackgroundColor(if (selected == 60)  activeColor else inactiveColor)
        binding.btnDuration120.setBackgroundColor(if (selected == 120) activeColor else inactiveColor)
    }

    private fun toggleSoundPreview() {
        if (previewMediaPlayer != null && previewMediaPlayer!!.isPlaying) {
            stopSoundPreview()
        } else {
            startSoundPreview()
        }
    }

    private fun startSoundPreview() {
        stopSoundPreview()
        try {
            previewMediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI)
            previewMediaPlayer?.apply {
                isLooping = false
                start()
            }
            binding.btnPreviewSound.text = getString(R.string.label_stop_sound)
            binding.btnPreviewSound.setIconResource(R.drawable.ic_stop_small)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSoundPreview() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            previewMediaPlayer = null
            binding.btnPreviewSound.text = getString(R.string.label_preview_sound)
            binding.btnPreviewSound.setIconResource(R.drawable.ic_play)
        }
    }

    private fun requestDetectionPermissions() {
        val perms = PermissionManager.requiredDetectionPermissions()
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            perms.any { shouldShowRequestPermissionRationale(it) }
        } else {
            false
        }

        if (shouldShowRationale) {
            showPermissionRationale()
        } else {
            permissionLauncher.launch(perms)
        }
    }

    private fun handlePermissionResult(micGranted: Boolean) {
        if (!micGranted) {
            val permanentlyDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            } else {
                false
            }

            if (permanentlyDenied) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.mic_permission_permanently_denied),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.open_settings)) {
                    PermissionManager.openAppPermissionSettings(this)
                }.show()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.mic_permission_denied),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } else {
            viewModel.refreshState()
        }
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            getString(R.string.mic_permission_rationale),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.allow)) {
            permissionLauncher.launch(PermissionManager.requiredDetectionPermissions())
        }.show()
    }
}
