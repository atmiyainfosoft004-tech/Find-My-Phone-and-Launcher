package com.example.findmyphonebyclaplauncher.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.databinding.ActivitySplashBinding
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity
import com.example.findmyphonebyclaplauncher.utils.PermissionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        // Set title with "clap" highlighted in primary brand color (#2563EB)
        val htmlTitle = "Just <font color='#2563EB'>clap</font> or whistle"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.txtTitle.text = Html.fromHtml(htmlTitle, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            binding.txtTitle.text = Html.fromHtml(htmlTitle)
        }

        setupVideoPlayer()

        binding.btnContinue.setOnClickListener {
            navigateNext()
        }
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

    private fun setupVideoPlayer() {
        if (binding.videoSplash.isAvailable) {
            binding.videoSplash.surfaceTexture?.let { surfaceTexture ->
                initMediaPlayer(surfaceTexture, binding.videoSplash.width, binding.videoSplash.height)
            }
        }

        binding.videoSplash.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                initMediaPlayer(surfaceTexture, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                adjustVideoAspect(width, height)
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                releaseMediaPlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
    }

    private fun initMediaPlayer(surfaceTexture: SurfaceTexture, viewWidth: Int, viewHeight: Int) {
        releaseMediaPlayer()
        try {
            val surface = Surface(surfaceTexture)
            val videoUri = Uri.parse("android.resource://$packageName/${R.raw.splash_video}")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, videoUri)
                setSurface(surface)
                isLooping = true
                setOnPreparedListener { mp ->
                    adjustVideoAspect(viewWidth, viewHeight)
                    mp.start()
                    binding.imgSplash.visibility = View.GONE
                }
                setOnErrorListener { _, _, _ ->
                    binding.imgSplash.visibility = View.VISIBLE
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.imgSplash.visibility = View.VISIBLE
        }
    }

    private fun adjustVideoAspect(viewWidth: Int, viewHeight: Int) {
        val mp = mediaPlayer ?: return
        try {
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

            val scaleX: Float
            val scaleY: Float
            val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
            val videoAspect = videoWidth / videoHeight

            if (videoAspect > viewAspect) {
                scaleX = videoAspect / viewAspect
                scaleY = 1.0f
            } else {
                scaleX = 1.0f
                scaleY = viewAspect / videoAspect
            }

            val matrix = Matrix()
            matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
            binding.videoSplash.setTransform(matrix)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (mediaPlayer != null && !(mediaPlayer!!.isPlaying)) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    private fun navigateNext() {
        if (isFinishing || isDestroyed) return
        releaseMediaPlayer()

        val userPrefs = UserPreferencesDataSource(applicationContext)
        val isCompleted = userPrefs.isOnboardingCompleted
        val isSkipped = userPrefs.isOnboardingSkipped

        val targetClass = if (isCompleted || isSkipped) {
            FindPhoneActivity::class.java
        } else {
            OnboardingActivity::class.java
        }

        startActivity(Intent(this, targetClass))
        finish()
    }
}
