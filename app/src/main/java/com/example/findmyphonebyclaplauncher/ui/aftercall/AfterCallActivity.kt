package com.example.findmyphonebyclaplauncher.ui.aftercall

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.BannerAdLoader
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSensitivityActivity
import com.example.findmyphonebyclaplauncher.ui.settings.AlertSoundActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AfterCallActivity : BaseActivity() {

    private val viewModel: AfterCallViewModel by viewModels()
    private lateinit var adapter: CallHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_after_call)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val rootLayout = findViewById<View>(R.id.rootLayout)
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                v.updatePadding(top = statusBarInsets.top)
                insets
            }
        }

        val targetNumber = intent.getStringExtra("EXTRA_NUMBER")

        setupViews()
        observeViewModel()

        viewModel.fetchCallLogs(targetNumber)

        // Show Medium Rectangle Banner Ad (300x250)
        val adFrame = findViewById<FrameLayout>(R.id.bannerAdFrameLayout)
        val shimmerFrame = findViewById<View>(R.id.shimmerFrameLayout)
        if (adFrame != null && shimmerFrame != null) {
            BannerAdLoader.instance?.showBannerAfterCall(this, adFrame, shimmerFrame)
        }
    }

    private fun setupViews() {
        val btnAlertSound = findViewById<LinearLayout>(R.id.btn_alert_sound)
        val btnShare = findViewById<LinearLayout>(R.id.btn_share)
        val btnAlertSensitivity = findViewById<LinearLayout>(R.id.btn_alert_sensitivity)
        val rvHistory = findViewById<RecyclerView>(R.id.rv_call_history)
        val ivBack = findViewById<ImageView>(R.id.iv_back)

        ivBack?.setOnClickListener {
            finish()
        }

        adapter = CallHistoryAdapter { item ->
            viewModel.formatTime(item.date)
        }
        rvHistory?.layoutManager = LinearLayoutManager(this)
        rvHistory?.adapter = adapter

        btnAlertSound?.setOnClickListener {
            val intent = Intent(this, AlertSoundActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }

        btnAlertSensitivity?.setOnClickListener {
            val intent = Intent(this, AlertSensitivityActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }

        btnShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                val shareMessage = "Check out this great app: https://play.google.com/store/apps/details?id=$packageName"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun observeViewModel() {
        val tvName = findViewById<TextView>(R.id.tv_caller_name)
        val tvDetails = findViewById<TextView>(R.id.tv_call_details)
        val ivProfile = findViewById<ImageView>(R.id.iv_profile)

        lifecycleScope.launch {
            viewModel.lastCall.collectLatest { item ->
                if (item != null) {
                    tvName.text = if (!item.name.isNullOrEmpty()) item.name else item.number
                    val durationStr = viewModel.formatDuration(item.duration)
                    val timeStr = viewModel.formatTime(item.date)
                    val typeStr = viewModel.getCallTypeText(item.type)
                    tvDetails.text = "$typeStr • $durationStr • $timeStr"

                    if (!item.photoUri.isNullOrEmpty()) {
                        ivProfile.setImageURI(Uri.parse(item.photoUri))
                        ivProfile.setPadding(0, 0, 0, 0)
                        ivProfile.imageTintList = null
                    } else {
                        ivProfile.setImageResource(R.drawable.ic_profile)
                        ivProfile.setPadding(32, 32, 32, 32)
                        ivProfile.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.recentCalls.collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }
}
