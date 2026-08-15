package com.example.findmyphonebyclaplauncher.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Owns home-screen gesture arbitration:
 * - Any upward swipe → app drawer
 * - Clear mostly-horizontal swipe → ViewPager page change (fake-drag)
 */
class SwipeUpInterceptLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface Callback {
        fun onSwipeUpStart()
        fun onSwipeUpProgress(progress: Float)
        fun onSwipeUpEnd(progress: Float, velocityY: Float)
    }

    var callback: Callback? = null
    var enabledIntercept: Boolean = true
    var drawerHeightPx: Float = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val maxFling = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var tracking = false
    private var draggingVertical = false
    private var draggingHorizontal = false
    private var velocityTracker: VelocityTracker? = null

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (!enabledIntercept) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!enabledIntercept) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                lastX = ev.x
                tracking = true
                draggingVertical = false
                draggingHorizontal = false
                findViewPager()?.isUserInputEnabled = false
                recycleTracker()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return false
                velocityTracker?.addMovement(ev)
                if (draggingVertical || draggingHorizontal) return true
                return decideDirection(ev.x, ev.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!draggingVertical && !draggingHorizontal) {
                    restorePagerInputIfNeeded()
                }
                tracking = false
                recycleTracker()
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabledIntercept) return false
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                lastX = event.x
                tracking = true
                findViewPager()?.isUserInputEnabled = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!draggingVertical && !draggingHorizontal) {
                    if (!decideDirection(event.x, event.y)) return true
                }
                if (draggingVertical) {
                    val pulled = (startY - event.y).coerceAtLeast(0f)
                    val height = drawerHeightPx.takeIf { it > 0f } ?: height.toFloat()
                    val progress = (pulled / height).coerceIn(0f, 1f)
                    callback?.onSwipeUpProgress(progress)
                    return true
                }
                if (draggingHorizontal) {
                    updateFakeDrag(event.x)
                    return true
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when {
                    draggingVertical -> {
                        velocityTracker?.computeCurrentVelocity(1000, maxFling)
                        val velocityY = -(velocityTracker?.yVelocity ?: 0f)
                        val pulled = (startY - event.y).coerceAtLeast(0f)
                        val height = drawerHeightPx.takeIf { it > 0f } ?: height.toFloat()
                        val progress = (pulled / height).coerceIn(0f, 1f)
                        callback?.onSwipeUpEnd(progress, velocityY)
                    }
                    draggingHorizontal -> endFakeDrag()
                    else -> restorePagerInputIfNeeded()
                }
                cleanup(keepPagerDisabled = draggingVertical)
                return true
            }
        }
        return draggingVertical || draggingHorizontal
    }

    private fun decideDirection(x: Float, y: Float): Boolean {
        val absDx = abs(x - startX)
        val absDy = abs(y - startY)
        val upDy = startY - y

        if (absDx < touchSlop && absDy < touchSlop) return false

        if (upDy > touchSlop) {
            claimVertical(y)
            return true
        }

        if (absDx > touchSlop && absDx >= absDy * HORIZONTAL_MIN_RATIO) {
            claimHorizontal(x)
            return draggingHorizontal
        }

        return false
    }

    private fun claimVertical(y: Float) {
        if (draggingVertical) return
        endFakeDrag(silent = true)
        draggingVertical = true
        draggingHorizontal = false
        parent?.requestDisallowInterceptTouchEvent(true)
        findViewPager()?.let { pager ->
            pager.isUserInputEnabled = false
            (pager.getChildAt(0) as? RecyclerView)?.stopScroll()
            pager.setCurrentItem(pager.currentItem, false)
        }
        callback?.onSwipeUpStart()
        val pulled = (startY - y).coerceAtLeast(0f)
        val height = drawerHeightPx.takeIf { it > 0f } ?: height.toFloat()
        callback?.onSwipeUpProgress((pulled / height).coerceIn(0f, 1f))
    }

    private fun claimHorizontal(x: Float) {
        if (draggingHorizontal || draggingVertical) return
        val pager = findViewPager() ?: return
        if (!pager.beginFakeDrag()) {
            pager.isUserInputEnabled = true
            draggingHorizontal = false
            tracking = false
            return
        }
        draggingHorizontal = true
        lastX = startX
        parent?.requestDisallowInterceptTouchEvent(true)
        updateFakeDrag(x)
    }

    private fun updateFakeDrag(x: Float) {
        val pager = findViewPager() ?: return
        if (!pager.isFakeDragging) return
        pager.fakeDragBy(x - lastX)
        lastX = x
    }

    private fun endFakeDrag(silent: Boolean = false) {
        val pager = findViewPager() ?: return
        if (pager.isFakeDragging) pager.endFakeDrag()
        draggingHorizontal = false
        if (!silent) pager.isUserInputEnabled = true
    }

    private fun restorePagerInputIfNeeded() {
        val pager = findViewPager() ?: return
        if (!pager.isFakeDragging) pager.isUserInputEnabled = true
    }

    private fun findViewPager(): ViewPager2? = findViewPager(this)

    private fun findViewPager(group: ViewGroup): ViewPager2? {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is ViewPager2 -> return child
                is ViewGroup -> findViewPager(child)?.let { return it }
            }
        }
        return null
    }

    private fun cleanup(keepPagerDisabled: Boolean) {
        tracking = false
        draggingVertical = false
        draggingHorizontal = false
        parent?.requestDisallowInterceptTouchEvent(false)
        if (!keepPagerDisabled) restorePagerInputIfNeeded()
        recycleTracker()
    }

    private fun recycleTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    companion object {
        private const val HORIZONTAL_MIN_RATIO = 1.25f
    }
}
