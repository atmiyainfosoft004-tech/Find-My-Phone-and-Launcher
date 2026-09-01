package com.example.findmyphonebyclaplauncher.ui.launcher

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.FragmentLauncherHostBinding
import com.example.findmyphonebyclaplauncher.ui.common.SwipeDownDismissLayout
import com.example.findmyphonebyclaplauncher.ui.common.SwipeUpInterceptLayout
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.LauncherPagerAdapter
import com.example.findmyphonebyclaplauncher.ui.launcher.drawer.AppDrawerFragment
import com.example.findmyphonebyclaplauncher.ui.launcher.drawer.AppDrawerMotionController
import com.example.findmyphonebyclaplauncher.util.BlurHelper
import com.example.findmyphonebyclaplauncher.util.SystemWallpaperHelper
import kotlinx.coroutines.launch
import kotlin.math.abs

import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.ads.SwipeDirection
import com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity
import com.example.findmyphonebyclaplauncher.ui.launcher.dashboard.DashboardFragment
import com.example.findmyphonebyclaplauncher.ui.launcher.findphone.FindPhoneFragment

class LauncherHostFragment : Fragment() {

    private var _binding: FragmentLauncherHostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LauncherViewModel by activityViewModels()
    private lateinit var pagerAdapter: LauncherPagerAdapter
    private lateinit var drawerMotion: AppDrawerMotionController
    private var skipNextPageSelectedAd = true
    private var pageChangeFromUserSwipe = false
    private var currentPageIndex: Int = LauncherPagerAdapter.PAGE_HOME

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLauncherHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSystemWallpaper()
        setupPager()
        setupPageIndicator()
        setupEdgeTab()
        setupDrawerMotion()
        setupDrawerGestures()
        observe()
        setupBlur()
        LauncherAdsHelper.preloadInterstitial(requireActivity())

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val indicatorMargin = resources.getDimensionPixelSize(R.dimen.page_indicator_margin_bottom)
            (binding.glassPageIndicatorBlur.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                ?.let { lp ->
                    lp.bottomMargin = indicatorMargin + nav
                    binding.glassPageIndicatorBlur.layoutParams = lp
                }
            insets
        }

        val targetPage = activity?.intent?.getIntExtra(
            FindPhoneActivity.EXTRA_TARGET_PAGE,
            LauncherPagerAdapter.PAGE_HOME
        ) ?: LauncherPagerAdapter.PAGE_HOME

        currentPageIndex = targetPage
        binding.viewPager.setCurrentItem(targetPage, false)
        updateChrome(targetPage)

        binding.root.post {
            val h = binding.drawerContainer.height.toFloat().coerceAtLeast(1f)
            drawerMotion.setDrawerHeight(h)
            binding.swipeUpIntercept.drawerHeightPx = h
            drawerMotion.reset()
            updateChrome(binding.viewPager.currentItem)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSystemWallpaper()
        if (_binding != null) {
            val isDrawerOpen = ::drawerMotion.isInitialized && drawerMotion.isOpen
            (activity as? FindPhoneActivity)?.applyLauncherPageSystemBars(binding.viewPager.currentItem, isDrawerOpen = isDrawerOpen)
            when (binding.viewPager.currentItem) {
                LauncherPagerAdapter.PAGE_FIND_PHONE -> {
                    (childFragmentManager.findFragmentByTag("f${LauncherPagerAdapter.PAGE_FIND_PHONE}") as? FindPhoneFragment)?.loadBannerAd()
                }
                LauncherPagerAdapter.PAGE_DASHBOARD -> {
                    (childFragmentManager.findFragmentByTag("f${LauncherPagerAdapter.PAGE_DASHBOARD}") as? DashboardFragment)?.refreshNativeAd()
                }
            }
            if (isDrawerOpen) {
                (childFragmentManager.findFragmentByTag(DRAWER_TAG) as? AppDrawerFragment)?.loadBannerAd()
            }
        }
    }

    private fun loadSystemWallpaper() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            SystemWallpaperHelper.applyTo(binding.ivWallpaper)
        }
    }

    private fun setupPager() {
        pagerAdapter = LauncherPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.swipeUpIntercept.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        (binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            itemAnimator = null
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        binding.viewPager.setPageTransformer { page, position ->
            page.alpha = 1f - abs(position) * 0.1f
            val scale = 0.98f + (1f - abs(position).coerceAtMost(1f)) * 0.02f
            page.scaleX = scale
            page.scaleY = scale
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> pageChangeFromUserSwipe = true
                    ViewPager2.SCROLL_STATE_IDLE -> pageChangeFromUserSwipe = false
                }
            }

            override fun onPageSelected(position: Int) {
                val previousPage = currentPageIndex
                currentPageIndex = position

                val direction = when {
                    previousPage == LauncherPagerAdapter.PAGE_HOME && position == LauncherPagerAdapter.PAGE_DASHBOARD -> {
                        // Home -> Dashboard: Right-to-Left swipe
                        SwipeDirection.RIGHT_TO_LEFT
                    }
                    previousPage == LauncherPagerAdapter.PAGE_HOME && position == LauncherPagerAdapter.PAGE_FIND_PHONE -> {
                        // Home -> Phone: Left-to-Right swipe
                        SwipeDirection.LEFT_TO_RIGHT
                    }
                    else -> {
                        // Dashboard -> Home, Phone -> Home, or other transitions are NOT counted
                        null
                    }
                }

                Log.d("AdCounter", "LauncherHost: onPageSelected -> from page $previousPage to $position (direction: $direction, userSwipe: $pageChangeFromUserSwipe)")

                viewModel.setPage(position)
                updateChrome(position)
                updateSwipeEnabled()
                if (position == LauncherPagerAdapter.PAGE_DASHBOARD) {
                    (childFragmentManager.findFragmentByTag("f${LauncherPagerAdapter.PAGE_DASHBOARD}") as? DashboardFragment)?.refreshNativeAd()
                } else if (position == LauncherPagerAdapter.PAGE_FIND_PHONE) {
                    (childFragmentManager.findFragmentByTag("f${LauncherPagerAdapter.PAGE_FIND_PHONE}") as? FindPhoneFragment)?.loadBannerAd()
                }
                maybeShowSwipeAd(direction)
            }
        })
    }

    private fun maybeShowSwipeAd(direction: SwipeDirection?) {
        if (skipNextPageSelectedAd) {
            skipNextPageSelectedAd = false
            pageChangeFromUserSwipe = false
            return
        }
        if (!pageChangeFromUserSwipe || direction == null) return
        pageChangeFromUserSwipe = false
        val activity = activity ?: return
        LauncherAdsHelper.showSwipeAd(activity, direction)
    }

    private fun setupPageIndicator() {
        binding.btnPagePhone.setOnClickListener {
            forceCloseDrawer()
            binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_FIND_PHONE, true)
        }
        binding.btnPageHome.setOnClickListener {
            forceCloseDrawer()
            binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, true)
        }
        binding.btnPageApps.setOnClickListener {
            forceCloseDrawer()
            binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_DASHBOARD, true)
        }
    }

    private fun setupEdgeTab() {
        binding.edgeTab.setOnClickListener {
            forceCloseDrawer()
            binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_FIND_PHONE, true)
        }
    }

    private fun setupDrawerMotion() {
        if (childFragmentManager.findFragmentByTag(DRAWER_TAG) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.drawerHost, AppDrawerFragment(), DRAWER_TAG)
                .commitNow()
        }

        drawerMotion = AppDrawerMotionController(
            homeLayer = binding.swipeUpIntercept,
            blurTarget = binding.ivWallpaper,
            drawerContainer = binding.drawerContainer,
            drawerPanel = binding.drawerDismiss,
            scrim = binding.drawerScrim,
            chromeViews = listOf(binding.pageIndicatorBlur, binding.edgeTab)
        )
    }

    private fun setupDrawerGestures() {
        binding.swipeUpIntercept.callback = object : SwipeUpInterceptLayout.Callback {
            override fun onSwipeUpStart() {
                if (binding.viewPager.currentItem != LauncherPagerAdapter.PAGE_HOME) return
                if (drawerMotion.isAnimating) drawerMotion.cancelAnimations()
                ensureDrawerHeight()
                binding.viewPager.isUserInputEnabled = false
                drawerMotion.applyProgress(0f)
            }

            override fun onSwipeUpProgress(progress: Float) {
                if (drawerMotion.isAnimating) return
                drawerMotion.applyProgress(progress)
            }

            override fun onSwipeUpEnd(progress: Float, velocityY: Float) {
                val shouldOpen = progress > 0.28f || velocityY > 1200f
                if (shouldOpen) openAppDrawer() else closeAppDrawer()
            }
        }

        binding.drawerDismiss.callback = object : SwipeDownDismissLayout.Callback {
            override fun onSwipeDownProgress(translationY: Float) {
                if (drawerMotion.isAnimating) return
                ensureDrawerHeight()
                val height = binding.drawerContainer.height.toFloat().coerceAtLeast(1f)
                val progress = (1f - (translationY / height)).coerceIn(0f, 1f)
                drawerMotion.applyProgress(progress)
            }

            override fun onSwipeDownEnd(translationY: Float, velocityY: Float) {
                ensureDrawerHeight()
                val height = binding.drawerContainer.height.toFloat().coerceAtLeast(1f)
                val progress = (1f - (translationY / height)).coerceIn(0f, 1f)
                val shouldClose = progress < 0.78f || velocityY > 1200f
                if (shouldClose) closeAppDrawer() else openAppDrawer()
            }
        }

        binding.drawerScrim.setOnClickListener {
            if (drawerMotion.isOpen || drawerMotion.progress > 0.5f) {
                closeAppDrawer()
            }
        }
    }

    fun openAppDrawer() {
        val wasAlreadyOpen = viewModel.drawerOpen.value
        ensureDrawerHeight()
        binding.viewPager.isUserInputEnabled = false
        updateSwipeEnabled()
        drawerMotion.openAppDrawer {
            viewModel.setDrawerOpenState(true)
            updateSwipeEnabled()
            (activity as? FindPhoneActivity)?.applyLauncherPageSystemBars(binding.viewPager.currentItem, isDrawerOpen = true)
            if (!wasAlreadyOpen) {
                (childFragmentManager.findFragmentByTag(DRAWER_TAG) as? AppDrawerFragment)?.loadBannerAd(forceReload = true)
            }
        }
    }

    fun closeAppDrawer() {
        ensureDrawerHeight()
        drawerMotion.closeAppDrawer {
            viewModel.setDrawerOpenState(false)
            binding.viewPager.isUserInputEnabled = true
            updateSwipeEnabled()
            updateChrome(binding.viewPager.currentItem)
            (activity as? FindPhoneActivity)?.applyLauncherPageSystemBars(binding.viewPager.currentItem, isDrawerOpen = false)
        }
    }

    private fun forceCloseDrawer() {
        drawerMotion.reset()
        viewModel.setDrawerOpenState(false)
        binding.viewPager.isUserInputEnabled = true
        updateSwipeEnabled()
        updateChrome(binding.viewPager.currentItem)
        (activity as? FindPhoneActivity)?.applyLauncherPageSystemBars(binding.viewPager.currentItem, isDrawerOpen = false)
    }

    private fun ensureDrawerHeight() {
        val h = binding.drawerContainer.height.toFloat()
            .takeIf { it > 0f }
            ?: resources.displayMetrics.heightPixels.toFloat()
        drawerMotion.setDrawerHeight(h)
        binding.swipeUpIntercept.drawerHeightPx = h
    }

    private fun updateSwipeEnabled() {
        val onHome = binding.viewPager.currentItem == LauncherPagerAdapter.PAGE_HOME
        binding.swipeUpIntercept.enabledIntercept =
            onHome &&
                !drawerMotion.isOpen &&
                !drawerMotion.isAnimating &&
                drawerMotion.progress < 0.01f
        if (!onHome) {
            binding.viewPager.isUserInputEnabled = true
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentPage.collect { updateChrome(it) } }
                launch {
                    viewModel.openDrawerEvents.collect {
                        if (binding.viewPager.currentItem == LauncherPagerAdapter.PAGE_HOME) {
                            openAppDrawer()
                        }
                    }
                }
                launch {
                    viewModel.closeDrawerEvents.collect {
                        if (drawerMotion.isOpen || drawerMotion.progress > 0.01f) {
                            closeAppDrawer()
                        }
                    }
                }
            }
        }
    }

    private fun updateChrome(page: Int) {
        if (drawerMotion.isOpen || drawerMotion.progress > 0.02f || drawerMotion.isAnimating) return

        val active = ContextCompat.getColor(requireContext(), R.color.white)
        val inactive = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        binding.btnPagePhone.imageTintList = android.content.res.ColorStateList.valueOf(
            if (page == LauncherPagerAdapter.PAGE_FIND_PHONE) active else inactive
        )
        binding.btnPageHome.imageTintList = android.content.res.ColorStateList.valueOf(
            if (page == LauncherPagerAdapter.PAGE_HOME) active else inactive
        )
        binding.btnPageApps.imageTintList = android.content.res.ColorStateList.valueOf(
            if (page == LauncherPagerAdapter.PAGE_DASHBOARD) active else inactive
        )

        when (page) {
            LauncherPagerAdapter.PAGE_HOME -> {
                binding.glassPageIndicatorBlur.isVisible = true
                binding.pageIndicatorBlur.isVisible = true
                binding.pageIndicatorBlur.alpha = 1f
                binding.edgeTab.isVisible = true
                binding.edgeTab.alpha = 1f
            }
            else -> {
                binding.glassPageIndicatorBlur.isVisible = false
                binding.pageIndicatorBlur.isVisible = false
                binding.edgeTab.isVisible = false
            }
        }
        (activity as? com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity)?.applyLauncherPageSystemBars(page)
        updateSwipeEnabled()
    }

    fun reapplyPageSystemBars() {
        if (_binding == null) return
        (activity as? com.example.findmyphonebyclaplauncher.ui.findphone.FindPhoneActivity)?.applyLauncherPageSystemBars(binding.viewPager.currentItem)
    }

    private fun setupBlur() {
        val root = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        BlurHelper.setup(
            binding.glassPageIndicatorBlur,
            root,
            ContextCompat.getColor(requireContext(), R.color.glass_dark),
            18f
        )
    }

    fun onLauncherBackPressed(): Boolean {
        if (drawerMotion.isOpen || drawerMotion.progress > 0.01f) {
            closeAppDrawer()
            return true
        }
        return when (binding.viewPager.currentItem) {
            LauncherPagerAdapter.PAGE_HOME -> false
            else -> {
                binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, true)
                true
            }
        }
    }

    fun onHomePressed() {
        if (!isAdded || _binding == null) return
        forceCloseDrawer()
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        currentPageIndex = LauncherPagerAdapter.PAGE_HOME
        binding.viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, false)
        updateChrome(LauncherPagerAdapter.PAGE_HOME)
    }

    fun setPage(page: Int, smoothScroll: Boolean = false) {
        if (_binding == null) return
        forceCloseDrawer()
        currentPageIndex = page
        binding.viewPager.setCurrentItem(page, smoothScroll)
        updateChrome(page)
    }

    override fun onDestroyView() {
        if (::drawerMotion.isInitialized) drawerMotion.cancelAnimations()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DRAWER_TAG = "app_drawer"
    }
}
