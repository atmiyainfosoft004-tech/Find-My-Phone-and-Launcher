package com.example.findmyphonebyclaplauncher.ui.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.NativeAdLoader
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.databinding.ActivityAppInstallSuccessBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import java.io.File

class AppInstallSuccessActivity : BaseActivity() {

    private lateinit var binding: ActivityAppInstallSuccessBinding
    private var targetPackageName: String = ""
    private var actionType: String = ACTION_TYPE_INSTALLED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppInstallSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_TYPE_INSTALLED

        if (targetPackageName.isBlank()) {
            finish()
            return
        }

        bindAppDetails()
        loadNativeAd()

        val isUninstall = actionType == ACTION_TYPE_UNINSTALLED
        binding.btnBottomAction.text = if (isUninstall) getString(R.string.btn_got_it) else "Open"
        binding.btnBottomAction.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackageName)
            if (launchIntent != null && !isUninstall) {
                runCatching { startActivity(launchIntent) }
                finish()
            } else {
                finish()
            }
        }
    }

    private fun bindAppDetails() {
        val pm = packageManager
        val extraAppLabel = intent.getStringExtra(EXTRA_APP_LABEL)
        val extraIconPath = intent.getStringExtra(EXTRA_ICON_FILE_PATH)

        when (actionType) {
            ACTION_TYPE_INSTALLED -> {
                binding.tvTitle.text = getString(R.string.app_install_success_title)
                binding.securityBadge.visibility = View.VISIBLE
            }
            ACTION_TYPE_UPDATED -> {
                binding.tvTitle.text = getString(R.string.app_updated_success_title)
                binding.securityBadge.visibility = View.VISIBLE
            }
            ACTION_TYPE_UNINSTALLED -> {
                binding.tvTitle.text = getString(R.string.app_uninstalled_success_title)
                binding.securityBadge.visibility = View.GONE
                binding.btnOpen.visibility = View.GONE
            }
        }

        var labelResolved = extraAppLabel
        var iconLoaded = false
        val extraAppSize = intent.getStringExtra(EXTRA_APP_SIZE)
        var sizeText = extraAppSize

        if (!extraIconPath.isNullOrBlank()) {
            val file = File(extraIconPath)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.ivAppIcon.setImageBitmap(bitmap)
                    iconLoaded = true
                }
            }
        }

        if (actionType != ACTION_TYPE_UNINSTALLED) {
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(targetPackageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(targetPackageName, 0)
                }

                if (labelResolved.isNullOrBlank()) {
                    labelResolved = appInfo.loadLabel(pm).toString()
                }
                if (!iconLoaded) {
                    binding.ivAppIcon.setImageDrawable(appInfo.loadIcon(pm))
                    iconLoaded = true
                }

                if (sizeText.isNullOrBlank()) {
                    val apkFile = File(appInfo.sourceDir)
                    if (apkFile.exists()) {
                        val sizeFormatted = Formatter.formatShortFileSize(this, apkFile.length())
                        if (sizeFormatted.isNotEmpty()) {
                            sizeText = "Size: $sizeFormatted"
                        }
                    }
                }

                val launchIntent = pm.getLaunchIntentForPackage(targetPackageName)
                if (launchIntent != null) {
                    binding.btnOpen.visibility = View.VISIBLE
                    binding.btnOpen.setOnClickListener {
                        runCatching { startActivity(launchIntent) }
                        finish()
                    }
                } else {
                    binding.btnOpen.visibility = View.GONE
                }
            } catch (_: Exception) {
                binding.btnOpen.visibility = View.GONE
            }
        } else {
            binding.btnOpen.visibility = View.GONE
        }

        binding.tvAppDetails.text = sizeText.takeIf { !it.isNullOrBlank() } ?: "Size: --"

        val displayLabel = labelResolved.takeIf { !it.isNullOrBlank() }
            ?: targetPackageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

        binding.tvAppName.text = displayLabel

        if (!iconLoaded) {
            binding.ivAppIcon.setImageResource(R.mipmap.ic_launcher)
        }
    }

    private fun loadNativeAd() {
        if (!com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(this) ||
            !AdsConfigManager.config.canShowNativeInstallUninstall
        ) {
            binding.nativeAdCardView.visibility = View.GONE
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            binding.nativeAdShimmerFrameLayout.visibility = View.GONE
            return
        }
        NativeAdLoader.instance?.showNativeLargeInstallUninstall(
            this,
            binding.nativeAdFrameLayout,
            binding.nativeAdShimmerFrameLayout,
            binding.nativeAdCardView
        )
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_ICON_FILE_PATH = "extra_icon_file_path"
        const val EXTRA_APP_SIZE = "extra_app_size"

        const val ACTION_TYPE_INSTALLED = "installed"
        const val ACTION_TYPE_UPDATED = "updated"
        const val ACTION_TYPE_UNINSTALLED = "uninstalled"

        fun start(
            context: Context,
            packageName: String,
            actionType: String,
            appLabel: String? = null,
            iconFilePath: String? = null,
            appSize: String? = null
        ) {
            val intent = Intent(context, AppInstallSuccessActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_ACTION_TYPE, actionType)
                if (!appLabel.isNullOrBlank()) putExtra(EXTRA_APP_LABEL, appLabel)
                if (!iconFilePath.isNullOrBlank()) putExtra(EXTRA_ICON_FILE_PATH, iconFilePath)
                if (!appSize.isNullOrBlank()) putExtra(EXTRA_APP_SIZE, appSize)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}
