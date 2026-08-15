package com.example.findmyphonebyclaplauncher.ui.launcher.drawer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.data.model.AppCategory
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.databinding.FragmentAppDrawerBinding
import com.example.findmyphonebyclaplauncher.ui.launcher.LauncherViewModel
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.AppIconAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper

import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager

class AppDrawerFragment : Fragment() {

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppDrawerViewModel by viewModels()
    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private lateinit var adapter: AppIconAdapter
    private var contextPopup: AppContextPopup? = null
    private var packageRemovedReceiver: BroadcastReceiver? = null
    private var pendingUninstallLabel: String? = null
    private var pendingUninstallPackage: String? = null
    private var uninstallInProgress = false
    private var favoritesTabVisible = false

    private val configChangeListener = AdsConfigManager.OnConfigChangeListener {
        loadBannerAd()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contextPopup = AppContextPopup(
            onAppInfo = viewModel::onOpenAppInfo,
            onToggleFavorite = viewModel::onToggleFavorite,
            onUninstall = ::startUninstall
        )
        adapter = AppIconAdapter(
            onClick = { app ->
                LauncherAdsHelper.showAppClickInterThen(requireActivity()) {
                    viewModel.openApp(app)
                    launcherViewModel.requestCloseDrawer()
                }
            },
            onLongClick = { app, anchor ->
                contextPopup?.show(anchor, app)
            }
        )
        binding.rvApps.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvApps.adapter = adapter
        binding.rvApps.itemAnimator = null
        binding.rvApps.setHasFixedSize(true)
        binding.rvApps.setItemViewCacheSize(24)

        setupTabs(showFavorites = false)
        setupSearch()
        observe()
        AdsConfigManager.addConfigChangeListener(configChangeListener)
    }

    fun loadBannerAd() {
        val binding = _binding ?: return
        if (!isAdded) return
        if (!AdsConfigManager.config.canShowBanner) {
            binding.drawerBanner.bannerAdFrameLayout.removeAllViews()
            binding.drawerBanner.bannerAdFrameLayout.visibility = View.GONE
            binding.drawerBanner.bannerAdShimmerFrameLayout.visibility = View.GONE
            return
        }
        if (binding.drawerBanner.bannerAdFrameLayout.childCount > 0) return
        LauncherAdsHelper.showDrawerBanner(
            requireActivity(),
            binding.drawerBanner.bannerAdFrameLayout,
            binding.drawerBanner.bannerAdShimmerFrameLayout
        )
    }

    override fun onResume() {
        super.onResume()
        uninstallInProgress = false
        viewModel.refreshApps()
        loadBannerAd()
    }

    private fun setupTabs(showFavorites: Boolean) {
        favoritesTabVisible = showFavorites
        val selected = viewModel.selectedCategory.value
        binding.tabCategories.clearOnTabSelectedListeners()
        binding.tabCategories.removeAllTabs()

        val tabs = buildList {
            add(AppCategory.ALL to getString(R.string.tab_all))
            if (showFavorites) {
                add(AppCategory.FAVORITES to getString(R.string.tab_favorites))
            }
            add(AppCategory.AUDIO to getString(R.string.tab_audio))
            add(AppCategory.GAMES to getString(R.string.tab_games))
            add(AppCategory.PHOTOGRAPHY to getString(R.string.tab_photography))
            add(AppCategory.PRODUCTIVITY to getString(R.string.tab_productivity))
            add(AppCategory.SOCIAL to getString(R.string.tab_social))
            add(AppCategory.VIDEO to getString(R.string.tab_video))
            add(AppCategory.OTHERS to getString(R.string.tab_others))
        }

        var selectedIndex = 0
        tabs.forEachIndexed { index, (category, title) ->
            binding.tabCategories.addTab(
                binding.tabCategories.newTab().setText(title).setTag(category),
                false
            )
            if (category == selected) selectedIndex = index
        }

        val safeIndex = when {
            selected == AppCategory.FAVORITES && !showFavorites -> 0
            else -> selectedIndex
        }
        binding.tabCategories.getTabAt(safeIndex)?.select()
        val resolvedCategory = binding.tabCategories.getTabAt(safeIndex)?.tag as? AppCategory
            ?: AppCategory.ALL
        viewModel.onCategorySelected(resolvedCategory)

        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.onCategorySelected(tab.tag as AppCategory)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            viewModel.onQueryChanged(query)
            binding.btnClearSearch.visibility =
                if (query.isNotEmpty()) View.VISIBLE else View.GONE
            if (query.isBlank()) {
                showAppsListUi()
            } else {
                updateEmptyState(appsEmpty = adapter.currentList.isEmpty())
            }
        }
        binding.btnClearSearch.setOnClickListener {
            clearSearchAndRestoreApps()
        }
    }

    private fun clearSearchAndRestoreApps() {
        binding.etSearch.setText("")
        viewModel.onQueryChanged("")
        selectAllCategoryTab()
        showAppsListUi()
        hideKeyboard()
    }

    private fun selectAllCategoryTab() {
        viewModel.onCategorySelected(AppCategory.ALL)
        for (i in 0 until binding.tabCategories.tabCount) {
            val tab = binding.tabCategories.getTabAt(i) ?: continue
            if (tab.tag == AppCategory.ALL) {
                tab.select()
                break
            }
        }
    }

    private fun showAppsListUi() {
        binding.emptySearchState.visibility = View.GONE
        binding.tabCategories.visibility = View.VISIBLE
        binding.rvApps.visibility = View.VISIBLE
        binding.etSearch.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
    }

    private fun updateEmptyState(appsEmpty: Boolean) {
        val query = binding.etSearch.text?.toString().orEmpty().trim()
        val showSearchEmpty = appsEmpty && query.isNotEmpty()

        if (!showSearchEmpty) {
            showAppsListUi()
            return
        }

        binding.emptySearchState.visibility = View.VISIBLE
        binding.tabCategories.visibility = View.GONE
        binding.rvApps.visibility = View.INVISIBLE
        binding.tvEmptyTitle.text = "No apps found matching: $query"
        binding.tvEmptySubtitle.visibility = View.VISIBLE
    }

    private fun focusSearchField() {
        val editText = _binding?.etSearch ?: return
        editText.post {
            if (_binding == null) return@post
            editText.requestFocus()
            editText.setSelection(editText.text?.length ?: 0)
            val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val editText = _binding?.etSearch ?: return
        editText.clearFocus()
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun startUninstall(app: AppInfo) {
        if (!app.canUninstall || uninstallInProgress) return
        uninstallInProgress = true
        contextPopup?.dismiss()

        pendingUninstallPackage = app.packageName
        pendingUninstallLabel = app.label
        registerPackageRemovedReceiver()

        val uri = Uri.parse("package:${app.packageName}")
        val intent = Intent(Intent.ACTION_DELETE, uri)
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
                launch {
                    viewModel.apps.collect { list ->
                        adapter.submitList(list)
                        updateEmptyState(appsEmpty = list.isEmpty())
                    }
                }
                launch {
                    viewModel.hasFavorites.collect { hasFavorites ->
                        if (hasFavorites != favoritesTabVisible) {
                            setupTabs(showFavorites = hasFavorites)
                        }
                    }
                }
                launch {
                    launcherViewModel.focusDrawerSearchEvents.collect {
                        focusSearchField()
                    }
                }
                launch {
                    launcherViewModel.drawerOpen.collect { open ->
                        if (!open) hideKeyboard()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        AdsConfigManager.removeConfigChangeListener(configChangeListener)
        unregisterPackageRemovedReceiver()
        contextPopup?.dismiss()
        contextPopup = null
        super.onDestroyView()
        _binding = null
    }
}
