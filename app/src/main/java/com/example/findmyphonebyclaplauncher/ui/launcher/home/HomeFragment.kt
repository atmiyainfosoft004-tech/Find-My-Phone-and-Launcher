package com.example.findmyphonebyclaplauncher.ui.launcher.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.databinding.FragmentHomeBinding
import com.example.findmyphonebyclaplauncher.ui.launcher.LauncherViewModel
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.DockAdapter
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.WorkspaceIconAdapter
import com.example.findmyphonebyclaplauncher.ui.launcher.drawer.AppContextPopup
import com.example.findmyphonebyclaplauncher.ui.search.GoogleSearchActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private lateinit var workspaceAdapter: WorkspaceIconAdapter
    private lateinit var dockAdapter: DockAdapter
    private var contextPopup: AppContextPopup? = null
    private var packageRemovedReceiver: BroadcastReceiver? = null
    private var pendingUninstallLabel: String? = null
    private var pendingUninstallPackage: String? = null
    private var uninstallInProgress = false

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            _binding?.tvClock?.text = viewModel.currentTime()
            _binding?.tvHomeDate?.text = viewModel.currentDate()
            clockHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disable app exit / back navigation on HomeFragment
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Keep the user on HomeFragment without exiting or navigating backward
            }
        })

        contextPopup = AppContextPopup(
            onAppInfo = viewModel::onOpenAppInfo,
            onToggleFavorite = viewModel::onToggleFavorite,
            onUninstall = ::startUninstall
        )

        workspaceAdapter = WorkspaceIconAdapter(
            onClick = { app ->
                LauncherAdsHelper.showAppClickInterThen(requireActivity()) { viewModel.openApp(app) }
            },
            onLongClick = { app, anchor -> contextPopup?.show(anchor, app) }
        )
        binding.rvWorkspace.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvWorkspace.adapter = workspaceAdapter
        binding.rvWorkspace.itemAnimator = null
        binding.rvWorkspace.setHasFixedSize(false)
        binding.rvWorkspace.isNestedScrollingEnabled = false
        binding.rvWorkspace.elevation = 12f

        dockAdapter = DockAdapter(
            onClick = { app ->
                LauncherAdsHelper.showAppClickInterThen(requireActivity()) { viewModel.openApp(app) }
            },
            onLongClick = { app, anchor -> contextPopup?.show(anchor, app) }
        )
        binding.rvDock.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvDock.adapter = dockAdapter
        binding.rvDock.itemAnimator = null
        binding.rvDock.setHasFixedSize(true)

        binding.tvClock.text = viewModel.currentTime()
        binding.tvHomeDate.text = viewModel.currentDate()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val dockMargin = resources.getDimensionPixelSize(R.dimen.dock_margin_bottom)
            (binding.dockContainer.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                ?.let { lp ->
                    lp.bottomMargin = dockMargin + nav
                    binding.dockContainer.layoutParams = lp
                }
            insets
        }

        setupClicks()
        observe()

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(220).start()
    }

    private fun setupClicks() {
        binding.googleSearchBar.setOnClickListener {
            GoogleSearchActivity.start(requireActivity())
        }
        binding.btnMic.setOnClickListener { openGoogleVoice() }
        binding.glassSearchBar.setOnClickListener {
            launcherViewModel.requestOpenDrawerWithSearch()
        }
    }

    private fun openGoogleVoice() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .recoverCatching {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    private fun startUninstall(app: AppInfo) {
        if (!app.canUninstall || uninstallInProgress) return
        uninstallInProgress = true
        contextPopup?.dismiss()

        pendingUninstallPackage = app.packageName
        pendingUninstallLabel = app.label
        registerPackageRemovedReceiver()

        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
        try {
            startActivity(intent)
        } catch (_: Exception) {
            uninstallInProgress = false
            pendingUninstallPackage = null
            pendingUninstallLabel = null
            unregisterPackageRemovedReceiver()
        }
    }

    private fun registerPackageRemovedReceiver() {
        unregisterPackageRemovedReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                val removedPackage = intent.data?.schemeSpecificPart ?: return
                if (removedPackage != pendingUninstallPackage) return
                pendingUninstallPackage = null
                pendingUninstallLabel = null
                uninstallInProgress = false
                viewModel.refreshApps()
                unregisterPackageRemovedReceiver()
            }
        }
        packageRemovedReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            requireContext().applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterPackageRemovedReceiver() {
        packageRemovedReceiver?.let { receiver ->
            runCatching {
                requireContext().applicationContext.unregisterReceiver(receiver)
            }
        }
        packageRemovedReceiver = null
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.workspaceApps.collect { workspaceAdapter.submitList(it) } }
                launch { viewModel.dockApps.collect { dockAdapter.submitList(it) } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uninstallInProgress = false
        clockHandler.post(clockTick)
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onDestroyView() {
        unregisterPackageRemovedReceiver()
        contextPopup?.dismiss()
        contextPopup = null
        clockHandler.removeCallbacks(clockTick)
        super.onDestroyView()
        _binding = null
    }
}
