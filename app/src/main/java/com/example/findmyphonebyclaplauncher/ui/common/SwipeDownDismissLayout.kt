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
import kotlin.math.abs

/**
 * Interactive swipe-down dismiss: drawer follows the finger.
 * Only intercepts when nested list cannot scroll upward.
 */
class SwipeDownDismissLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface Callback {
        fun onSwipeDownProgress(translationY: Float)
        fun onSwipeDownEnd(translationY: Float, velocityY: Float)
    }

    var callback: Callback? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val maxFling = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()

    private var startY = 0f
    private var dragging = false
    private var decided = false
    private var velocityTracker: VelocityTracker? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val scrollChild = findScrollableChild()
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.rawY
                dragging = false
                decided = false
                recycleTracker()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                val dy = ev.rawY - startY
                if (!decided && abs(dy) > touchSlop) {
                    decided = true
                    val canScrollUp = scrollChild?.canScrollVertically(-1) == true
                    if (dy > 0 && !canScrollUp) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
                return dragging
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                decided = false
                recycleTracker()
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocityTracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    if (event.rawY - startY > touchSlop) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else return true
                }
                val translation = (event.rawY - startY).coerceAtLeast(0f)
                callback?.onSwipeDownProgress(translation)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    velocityTracker?.computeCurrentVelocity(1000, maxFling)
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    val translation = (event.rawY - startY).coerceAtLeast(0f)
                    callback?.onSwipeDownEnd(translation, velocityY)
                }
                dragging = false
                decided = false
                parent?.requestDisallowInterceptTouchEvent(false)
                recycleTracker()
                return true
            }
        }
        return true
    }

    private fun findScrollableChild(): View? {
        val root = getChildAt(0) as? ViewGroup ?: return getChildAt(0)
        return findRecycler(root) ?: root
    }

    private fun findRecycler(group: ViewGroup): View? {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is androidx.recyclerview.widget.RecyclerView) return child
            if (child is ViewGroup) findRecycler(child)?.let { return it }
        }
        return null
    }

    private fun recycleTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }
}
