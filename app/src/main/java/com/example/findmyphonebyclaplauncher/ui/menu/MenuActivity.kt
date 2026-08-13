package com.example.findmyphonebyclaplauncher.ui.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.findmyphonebyclaplauncher.databinding.ActivityMenuBinding
import com.google.android.material.snackbar.Snackbar

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
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
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.cardAbout.setOnClickListener {
            Snackbar.make(binding.root, "Find My Phone Launcher v1.0", Snackbar.LENGTH_SHORT).show()
        }

        binding.cardHelp.setOnClickListener {
            Snackbar.make(binding.root, "Clap 3 times or whistle out loud to locate your phone.", Snackbar.LENGTH_SHORT).show()
        }

        binding.cardPrivacy.setOnClickListener {
            Snackbar.make(binding.root, "Your privacy is protected. No audio is saved or uploaded.", Snackbar.LENGTH_SHORT).show()
        }

        binding.cardTerms.setOnClickListener {
            Snackbar.make(binding.root, "Terms of Service: Standard launcher and alert usage.", Snackbar.LENGTH_SHORT).show()
        }
    }
}
