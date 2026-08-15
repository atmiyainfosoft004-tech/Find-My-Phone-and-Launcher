package com.example.findmyphonebyclaplauncher.ui.launcher.findphone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivityFindPhoneBinding
import com.example.findmyphonebyclaplauncher.databinding.DialogAlertDurationBinding
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneViewModel
import com.example.findmyphonebyclaplauncher.ui.menu.MenuActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSensitivityActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSoundActivity
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import com.google.android.material.snackbar.Snackbar

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.util.startActivityWithSlideAnimation

class FindPhoneFragment : Fragment() {

    private var _binding: ActivityFindPhoneBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FindPhoneViewModel by viewModels()
    private lateinit var prefs: UserPreferencesDataSource

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        handlePermissionResult(micGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFindPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = UserPreferencesDataSource(requireContext())

        setupWindowInsets()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutHeader.setPadding(
                binding.layoutHeader.paddingLeft,
                systemBars.top + resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                binding.layoutHeader.paddingRight,
                binding.layoutHeader.paddingBottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState(requireContext())
        refreshSettingsUI()
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
        binding.btnMenu.setOnClickListener {
            requireContext().startActivityWithSlideAnimation(Intent(requireContext(), MenuActivity::class.java))
        }

        binding.switchMasterDetection.setOnClickListener {
            val isChecked = binding.switchMasterDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(requireContext())) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setMasterDetection(requireContext(), newState)
        }

        binding.switchClapDetection.setOnClickListener {
            val isMasterOn = binding.switchMasterDetection.tag as? Boolean ?: false
            if (!isMasterOn) {
                Snackbar.make(binding.root, getString(R.string.enable_master_detection_first), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isChecked = binding.switchClapDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(requireContext())) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setClapDetection(requireContext(), newState)
        }

        binding.switchWhistleDetection.setOnClickListener {
            val isMasterOn = binding.switchMasterDetection.tag as? Boolean ?: false
            if (!isMasterOn) {
                Snackbar.make(binding.root, getString(R.string.enable_master_detection_first), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isChecked = binding.switchWhistleDetection.tag as? Boolean ?: false
            val newState = !isChecked
            if (newState && !PermissionManager.hasRecordAudioPermission(requireContext())) {
                requestDetectionPermissions()
                return@setOnClickListener
            }
            viewModel.setWhistleDetection(requireContext(), newState)
        }

        binding.cardAlertSound.setOnClickListener {
            requireContext().startActivityWithSlideAnimation(Intent(requireContext(), AlertSoundActivity::class.java))
        }

        binding.cardAlertSensitivity.setOnClickListener {
            requireContext().startActivityWithSlideAnimation(Intent(requireContext(), AlertSensitivityActivity::class.java))
        }

        binding.cardAlertDuration.setOnClickListener {
            showAlertDurationDialog()
        }

        binding.switchFlashlight.setOnClickListener {
            val isChecked = binding.switchFlashlight.tag as? Boolean ?: false
            viewModel.setFlashlightEnabled(!isChecked)
        }

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
            val ctx = requireContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                manager.defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
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
        viewModel.isClapEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchClapDetection.setSwitchState(enabled)
        }

        viewModel.isWhistleEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchWhistleDetection.setSwitchState(enabled)
        }

        viewModel.isServiceRunning.observe(viewLifecycleOwner) { running ->
            binding.switchMasterDetection.setSwitchState(running)
            binding.cardClap.alpha = if (running) 1.0f else 0.5f
            binding.cardWhistle.alpha = if (running) 1.0f else 0.5f
            binding.switchClapDetection.isEnabled = running
            binding.switchWhistleDetection.isEnabled = running
        }

        viewModel.isFlashlightEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchFlashlight.setSwitchState(enabled)
        }

        viewModel.isVibrationEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchVibration.setSwitchState(enabled)
        }

        viewModel.selectedAlertDuration.observe(viewLifecycleOwner) { duration ->
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
                    PermissionManager.openAppPermissionSettings(requireContext())
                }.show()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.mic_permission_denied),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } else {
            viewModel.refreshState(requireContext())
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

    private fun showAlertDurationDialog() {
        val dialogBinding = DialogAlertDurationBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.decorView.background = ColorDrawable(Color.TRANSPARENT)
        }

        val fontBold = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
        val fontMedium = ResourcesCompat.getFont(requireContext(), R.font.inter_medium)

        val radioButtons = listOf(
            dialogBinding.radio10,
            dialogBinding.radio30,
            dialogBinding.radio60,
            dialogBinding.radio120
        )

        fun updateFontStyles(checkedId: Int) {
            for (rb in radioButtons) {
                if (rb.id == checkedId) {
                    rb.typeface = fontBold
                } else {
                    rb.typeface = fontMedium
                }
            }
        }

        val currentDuration = prefs.selectedAlertDuration
        when (currentDuration) {
            10   -> dialogBinding.radio10.isChecked = true
            60   -> dialogBinding.radio60.isChecked = true
            120  -> dialogBinding.radio120.isChecked = true
            else -> dialogBinding.radio30.isChecked = true
        }

        updateFontStyles(dialogBinding.radioGroupDuration.checkedRadioButtonId)

        dialogBinding.radioGroupDuration.setOnCheckedChangeListener { _, checkedId ->
            updateFontStyles(checkedId)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnOk.setOnClickListener {
            val selectedDuration = when (dialogBinding.radioGroupDuration.checkedRadioButtonId) {
                R.id.radio10  -> 10
                R.id.radio60  -> 60
                R.id.radio120 -> 120
                else          -> 30
            }
            viewModel.setSelectedAlertDuration(selectedDuration)
            binding.txtSelectedDuration.text = "$selectedDuration Sec"
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.let { window ->
            val marginPx = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            val targetWidth = (resources.displayMetrics.widthPixels - (marginPx * 2)).coerceAtLeast(1)
            window.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
