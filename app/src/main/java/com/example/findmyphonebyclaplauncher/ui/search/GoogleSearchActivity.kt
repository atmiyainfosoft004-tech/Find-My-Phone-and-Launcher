package com.example.findmyphonebyclaplauncher.ui.search

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.databinding.ActivityGoogleSearchBinding
import com.example.findmyphonebyclaplauncher.ui.common.BaseActivity
import com.example.findmyphonebyclaplauncher.util.ChromeCustomTabHelper
import kotlinx.coroutines.launch

class GoogleSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityGoogleSearchBinding
    private val viewModel: GoogleSearchViewModel by viewModels()
    private lateinit var feedAdapter: GoogleSearchFeedAdapter
    private var waitingForBlogReturn = false
    private var blogOverlayClosed = false
    private var blogPausedAt = 0L
    private var showInterAttempts = 0

    private val webViewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onBlogOverlayClosed()
    }

    private val showBlogReturnInterRunnable: Runnable = Runnable {
        if (isFinishing || isDestroyed) return@Runnable
        if (!waitingForBlogReturn) return@Runnable
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (showInterAttempts++ < 15) {
                binding.root.postDelayed(showBlogReturnInterRunnable, 200)
            }
            return@Runnable
        }
        waitingForBlogReturn = false
        blogOverlayClosed = false
        showInterAttempts = 0
        LauncherAdsHelper.showBlogReturnInter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setBackgroundDrawableResource(R.color.drawer_panel_bg)

        feedAdapter = GoogleSearchFeedAdapter(this) { post -> openBlog(post.url) }
        binding.rvFeed.layoutManager = LinearLayoutManager(this)
        binding.rvFeed.adapter = feedAdapter
        binding.rvFeed.itemAnimator = null

        binding.etSearch.doAfterTextChanged { editable ->
            viewModel.onQueryChanged(editable?.toString().orEmpty())
            updateTrailingIcons(editable?.toString().orEmpty())
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch(binding.etSearch.text?.toString().orEmpty())
                true
            } else false
        }

        binding.btnClearQuery.setOnClickListener {
            binding.etSearch.setText("")
            binding.etSearch.requestFocus()
            showKeyboard()
        }
        binding.btnVoice.setOnClickListener { openVoiceSearch() }
        binding.btnClearRecent.setOnClickListener { viewModel.clearRecent() }
        binding.etSearch.clearFocus()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeScreen()
                }
            }
        )

        observe()
        ChromeCustomTabHelper.warmup(this)
        LauncherAdsHelper.preloadInterstitial(this)
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recent.collect { recent ->
                        val showingSuggestions = viewModel.query.value.trim().isNotEmpty()
                        bindRecent(recent, show = !showingSuggestions)
                    }
                }
                launch {
                    viewModel.suggestions.collect { suggestions ->
                        val q = viewModel.query.value.trim()
                        bindSuggestions(suggestions, show = q.isNotEmpty())
                        bindRecent(viewModel.recent.value, show = q.isEmpty())
                        binding.rvFeed.isVisible = q.isEmpty()
                    }
                }
                launch {
                    viewModel.feed.collect { feedAdapter.submitList(it) }
                }
            }
        }
    }

    private fun bindRecent(items: List<String>, show: Boolean) {
        binding.cardRecent.isVisible = show && items.isNotEmpty()
        if (!binding.cardRecent.isVisible) return
        binding.recentList.removeAllViews()
        items.forEachIndexed { index, query ->
            binding.recentList.addView(
                createQueryRow(
                    text = query,
                    leadingRes = R.drawable.ic_history_clock,
                    showDivider = index < items.lastIndex,
                    onClick = { submitSearch(query) },
                    onFill = { fillQuery(query) }
                )
            )
        }
    }

    private fun bindSuggestions(items: List<String>, show: Boolean) {
        binding.cardSuggestions.isVisible = show && items.isNotEmpty()
        if (!binding.cardSuggestions.isVisible) {
            binding.suggestionsList.removeAllViews()
            return
        }
        binding.suggestionsList.removeAllViews()
        items.forEachIndexed { index, suggestion ->
            binding.suggestionsList.addView(
                createQueryRow(
                    text = suggestion,
                    leadingRes = R.drawable.ic_search,
                    showDivider = index < items.lastIndex,
                    onClick = { submitSearch(suggestion) },
                    onFill = { fillQuery(suggestion) }
                )
            )
        }
    }

    private fun createQueryRow(
        text: String,
        leadingRes: Int,
        showDivider: Boolean,
        onClick: () -> Unit,
        onFill: () -> Unit
    ): View {
        val row = layoutInflater.inflate(R.layout.item_search_query_row, binding.recentList, false)
        row.findViewById<TextView>(R.id.tvQuery).text = text
        val leading = row.findViewById<ImageView>(R.id.ivLeading)
        leading.setImageResource(leadingRes)
        leading.imageTintList = ContextCompat.getColorStateList(
            this,
            if (leadingRes == R.drawable.ic_search) R.color.white else R.color.text_secondary
        )
        row.findViewById<ImageView>(R.id.btnFill).setOnClickListener { onFill() }
        row.setOnClickListener { onClick() }
        if (showDivider) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.displayMetrics.density.toInt().coerceAtLeast(1)
                )
                setBackgroundColor(ContextCompat.getColor(this@GoogleSearchActivity, R.color.google_search_divider))
            }
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(row)
                addView(divider)
            }
        }
        return row
    }

    private fun fillQuery(text: String) {
        binding.etSearch.setText(text)
        binding.etSearch.setSelection(text.length)
        binding.etSearch.requestFocus()
        showKeyboard()
    }

    private fun submitSearch(raw: String) {
        val query = raw.trim()
        if (query.isEmpty()) return
        viewModel.rememberSearch(query)
        hideKeyboard()
        openUrl(viewModel.googleSearchUrl(query))
    }

    private fun updateTrailingIcons(text: String) {
        val hasText = text.isNotEmpty()
        binding.btnClearQuery.isVisible = hasText
        binding.btnVoice.isVisible = !hasText
    }

    private fun openBlog(url: String) {
        if (url.isBlank()) return
        waitingForBlogReturn = true
        blogOverlayClosed = false
        blogPausedAt = 0L
        showInterAttempts = 0
        val openedChrome = ChromeCustomTabHelper.openUrl(this, url) {
            onBlogOverlayClosed()
        }
        if (!openedChrome) {
            webViewLauncher.launch(WebViewActivity.createIntent(this, url))
        }
    }

    private fun openUrl(url: String) {
        val openedChrome = ChromeCustomTabHelper.openUrl(this, url)
        if (!openedChrome && url.isNotBlank()) {
            WebViewActivity.start(this, url)
        }
    }

    private fun onBlogOverlayClosed() {
        if (!waitingForBlogReturn) return
        blogOverlayClosed = true
        scheduleBlogReturnInter()
    }

    private fun scheduleBlogReturnInter() {
        if (!waitingForBlogReturn || !blogOverlayClosed) return
        if (isFinishing || isDestroyed) return
        showInterAttempts = 0
        binding.root.removeCallbacks(showBlogReturnInterRunnable)
        binding.root.postDelayed(showBlogReturnInterRunnable, 300)
    }

    private fun openVoiceSearch() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .recoverCatching { openUrl("https://www.google.com") }
    }

    private fun showKeyboard() {
        binding.etSearch.post {
            val imm = getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun closeScreen() {
        hideKeyboard()
        LauncherAdsHelper.showBackAd(this) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    override fun onPause() {
        if (waitingForBlogReturn) blogPausedAt = SystemClock.elapsedRealtime()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRecent()
        if (waitingForBlogReturn &&
            !blogOverlayClosed &&
            blogPausedAt > 0L &&
            SystemClock.elapsedRealtime() - blogPausedAt >= 800L
        ) {
            blogOverlayClosed = true
        }
        if (waitingForBlogReturn && blogOverlayClosed) {
            scheduleBlogReturnInter()
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.root.removeCallbacks(showBlogReturnInterRunnable)
        }
        ChromeCustomTabHelper.release(this)
        if (::feedAdapter.isInitialized) feedAdapter.destroyAds()
        super.onDestroy()
    }

    companion object {
        fun start(activity: android.app.Activity) {
            activity.startActivity(Intent(activity, GoogleSearchActivity::class.java))
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }
    }
}
