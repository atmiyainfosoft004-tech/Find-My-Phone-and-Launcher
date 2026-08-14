package com.example.findmyphonebyclaplauncher.ui.onboarding.fragments

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.FragmentOnboardingScreen1Binding
import com.example.findmyphonebyclaplauncher.ui.onboarding.OnboardingActivity

/**
 * First page in OnboardingActivity ViewPager2.
 * Displays the video background, title, subtitle, and advances to the next onboarding fragment.
 */
class OnboardingScreen1Fragment : Fragment() {

    private var _binding: FragmentOnboardingScreen1Binding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingScreen1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val htmlTitle = "Just <font color='#2563EB'>clap</font> or whistle"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.txtTitle.text = Html.fromHtml(htmlTitle, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            binding.txtTitle.text = Html.fromHtml(htmlTitle)
        }

        setupVideoPlayer()

        binding.btnContinue.setOnClickListener {
            (activity as? OnboardingActivity)?.goToNextPage()
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
        val ctx = context ?: return
        try {
            val surface = Surface(surfaceTexture)
            val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash_video}")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(ctx.applicationContext, videoUri)
                setSurface(surface)
                isLooping = true
                setOnPreparedListener { mp ->
                    adjustVideoAspect(viewWidth, viewHeight)
                    mp.start()
                }
                setOnErrorListener { _, _, _ ->
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun adjustVideoAspect(viewWidth: Int, viewHeight: Int) {
        val mp = mediaPlayer ?: return
        val videoSplash = _binding?.videoSplash ?: return
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
            videoSplash.setTransform(matrix)
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

    override fun onDestroyView() {
        releaseMediaPlayer()
        super.onDestroyView()
        _binding = null
    }
}
