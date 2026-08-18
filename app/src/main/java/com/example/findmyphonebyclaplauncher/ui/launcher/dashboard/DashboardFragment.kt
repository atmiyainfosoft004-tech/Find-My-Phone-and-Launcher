package com.example.findmyphonebyclaplauncher.ui.launcher.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.databinding.FragmentDashboardBinding
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.CompactAppIconAdapter
import com.example.findmyphonebyclaplauncher.util.BlurHelper
import com.example.findmyphonebyclaplauncher.util.GreetingHelper
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var suggestedAdapter: CompactAppIconAdapter
    private lateinit var recentAdapter: CompactAppIconAdapter

    private val configChangeListener = AdsConfigManager.OnConfigChangeListener {
        refreshNativeAd()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvGreeting.text = GreetingHelper.greeting()
        binding.tvDate.text = GreetingHelper.formattedDate()

        suggestedAdapter = CompactAppIconAdapter { app ->
            LauncherAdsHelper.showAppClickAd(requireActivity(), app.packageName) { viewModel.openApp(app) }
        }
        recentAdapter = CompactAppIconAdapter { app ->
            LauncherAdsHelper.showAppClickAd(requireActivity(), app.packageName) { viewModel.openApp(app) }
        }

        binding.rvSuggested.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSuggested.adapter = suggestedAdapter
        binding.rvSuggested.itemAnimator = null

        binding.rvRecentApps.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvRecentApps.adapter = recentAdapter
        binding.rvRecentApps.itemAnimator = null

        val root = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        val overlay = ContextCompat.getColor(requireContext(), R.color.glass_dark)
        listOf(binding.blurSuggested, binding.blurUsage, binding.blurRecent).forEach {
            BlurHelper.setup(it, root, overlay, 18f)
        }

        binding.btnGrantPermission.setOnClickListener {
            viewModel.openUsageAccessSettings(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.suggestedApps.collect { suggestedAdapter.submitList(it) } }
                launch { viewModel.recentApps.collect { recentAdapter.submitList(it) } }
                launch { viewModel.usageCard.collect { bindUsageCard(it) } }
            }
        }

        AdsConfigManager.addConfigChangeListener(configChangeListener)
        com.example.findmyphonebyclaplauncher.util.NetworkUtil.observeNetwork(
            requireContext(),
            viewLifecycleOwner,
            onAvailable = { refreshNativeAd() },
            onLost = { refreshNativeAd() }
        )

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(280).start()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUsageCard(requireContext())
        refreshNativeAd()
    }

    fun refreshNativeAd() {
        val binding = _binding ?: return
        if (!isAdded) return
        if (!com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(context) ||
            !AdsConfigManager.config.canShowNativeDashboard
        ) {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            binding.nativeAdShimmerFrameLayout.root.visibility = View.GONE
            binding.nativeAdCardView.visibility = View.GONE
            return
        }
        if (binding.nativeAdFrameLayout.childCount == 0) {
            LauncherAdsHelper.showDashboardNative(
                requireActivity(),
                binding.nativeAdFrameLayout,
                binding.nativeAdShimmerFrameLayout.root,
                binding.nativeAdCardView
            )
        }
    }

    private fun bindUsageCard(state: UsageCardState) {
        if (!state.hasPermission) {
            binding.tvUsageMessage.text = getString(R.string.usage_permission_required)
            binding.btnGrantPermission.isVisible = true
            return
        }
        binding.btnGrantPermission.isVisible = false
        val body = buildString {
            append(state.totalLabel)
            if (state.topLines.isNotEmpty()) {
                append("\n\n")
                append(state.topLines.joinToString("\n"))
            }
        }
        binding.tvUsageMessage.text = body
    }

    override fun onDestroyView() {
        AdsConfigManager.removeConfigChangeListener(configChangeListener)
        super.onDestroyView()
        _binding = null
    }
}
