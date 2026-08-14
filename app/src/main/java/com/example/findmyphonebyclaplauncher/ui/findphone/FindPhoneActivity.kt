package com.example.findmyphonebyclaplauncher.ui.findphone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityFindPhoneBinding
import com.example.findmyphonebyclaplauncher.ui.menu.MenuActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSensitivityActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSoundActivity
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import com.google.android.material.snackbar.Snackbar

class FindPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindPhoneBinding
    private val viewModel: FindPhoneViewModel by viewModels()
    private lateinit var prefs: UserPreferencesDataSource

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

        prefs = UserPreferencesDataSource(this)

        setupWindowInsets()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
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
        viewModel.refreshState(this)
        refreshSettingsUI()
        checkAndRequestPermissionsIfNeeded()
    }

    private fun refreshSettingsUI() {
        val soundName = when (prefs.selectedAlertSound.lowercase()) {
            "whistle"   -> getString(R.string.your_ringtone_single)
            "airhorn"   -> getString(R.string.air_horn).replace("\n", " ")
            "babylaugh" -> getString(R.string.baby_laugh).replace("\n", " ")
            "cat"       -> getString(R.string.cat)
            "dog"       -> getString(R.string.dog)
            "doorbell"  -> getString(R.string.door_bell).replace("\n", " ")
            "train"     -> getString(R.string.train)
            "hello"     -> getString(R.string.hello)
            "horn"      -> getString(R.string.horn)
            else        -> getString(R.string.your_ringtone_single)
        }
        binding.txtSelectedSound.text = soundName

        val vol = prefs.alertSoundVolume
        binding.seekBarVolumePreview.max = 100
        binding.seekBarVolumePreview.progress = vol
        binding.seekBarVolumePreview.isEnabled = false

        binding.txtSelectedSensitivity.text = prefs.alertSensitivity
        binding.txtSelectedDuration.text = "${prefs.selectedAlertDuration} Sec"
    }

    private fun checkAndRequestPermissionsIfNeeded() {
        if (!PermissionManager.hasRecordAudioPermission(this)) {
            requestDetectionPermissions()
        }
    }

    private fun AppCompatImageView.setSwitchState(isChecked: Boolean) {
        if (isChecked) {
            setImageResource(R.drawable.ic_switch_on)
            contentDescription = "Enabled"
        } else {
            setImageResource(R.drawable.ic_switch_off)
            contentDescription = "Disabled"
        }
        tag = isChecked
    }

    private fun setupListeners() {
        // Hamburger Menu click -> MenuActivity
        binding.btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Master Custom Switch
        binding.switchMasterDetection.setOnClickListener {
            val isChecked = binding.switchMasterDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(this)) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setMasterDetection(this, newState)
        }

        // Clap Custom Switch
        binding.switchClapDetection.setOnClickListener {
            val isMasterOn = binding.switchMasterDetection.tag as? Boolean ?: false
            if (!isMasterOn) {
                Snackbar.make(binding.root, getString(R.string.enable_master_detection_first), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isChecked = binding.switchClapDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(this)) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setClapDetection(this, newState)
        }

        // Whistle Custom Switch
        binding.switchWhistleDetection.setOnClickListener {
            val isMasterOn = binding.switchMasterDetection.tag as? Boolean ?: false
            if (!isMasterOn) {
                Snackbar.make(binding.root, getString(R.string.enable_master_detection_first), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isChecked = binding.switchWhistleDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(this)) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setWhistleDetection(this, newState)
        }

        // Alert Sound Card click -> AlertSoundActivity
        binding.cardAlertSound.setOnClickListener {
            startActivity(Intent(this, AlertSoundActivity::class.java))
        }

        // Alert Sensitivity Card click -> AlertSensitivityActivity
        binding.cardAlertSensitivity.setOnClickListener {
            startActivity(Intent(this, AlertSensitivityActivity::class.java))
        }

        // Alert Duration Card click -> Cycle durations (10s, 30s, 60s, 120s)
        binding.cardAlertDuration.setOnClickListener {
            val current = prefs.selectedAlertDuration
            val next = when (current) {
                10   -> 30
                30   -> 60
                60   -> 120
                else -> 10
            }
            viewModel.setSelectedAlertDuration(next)
            binding.txtSelectedDuration.text = "$next Sec"
        }

        // Flashlight Custom Switch
        binding.switchFlashlight.setOnClickListener {
            val isChecked = binding.switchFlashlight.tag as? Boolean ?: false
            viewModel.setFlashlightEnabled(!isChecked)
        }

        // Vibration Custom Switch
        binding.switchVibration.setOnClickListener {
            val isChecked = binding.switchVibration.tag as? Boolean ?: false
            val newState = !isChecked
            viewModel.setVibrationEnabled(newState)
            if (newState) {
                triggerHapticFeedback()
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                manager.defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeViewModel() {
        viewModel.isClapEnabled.observe(this) { enabled ->
            binding.switchClapDetection.setSwitchState(enabled)
        }

        viewModel.isWhistleEnabled.observe(this) { enabled ->
            binding.switchWhistleDetection.setSwitchState(enabled)
        }

        viewModel.isServiceRunning.observe(this) { running ->
            binding.switchMasterDetection.setSwitchState(running)
            binding.cardClap.alpha = if (running) 1.0f else 0.5f
            binding.cardWhistle.alpha = if (running) 1.0f else 0.5f
            binding.switchClapDetection.isEnabled = running
            binding.switchWhistleDetection.isEnabled = running
        }

        viewModel.isFlashlightEnabled.observe(this) { enabled ->
            binding.switchFlashlight.setSwitchState(enabled)
        }

        viewModel.isVibrationEnabled.observe(this) { enabled ->
            binding.switchVibration.setSwitchState(enabled)
        }

        viewModel.selectedAlertDuration.observe(this) { duration ->
            binding.txtSelectedDuration.text = "$duration Sec"
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
            viewModel.refreshState(this)
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
