package com.example.findmyphonebyclaplauncher.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import com.example.findmyphonebyclaplauncher.R

class AdLoadingDialog(private val activity: Activity) {

    private var dialog: Dialog? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    companion object {
        private const val TAG = "InterstitialAd"
    }

    fun show(timeoutMs: Long = 2500L, onTimeout: (() -> Unit)? = null) {
        if (activity.isFinishing || activity.isDestroyed) return
        dismiss()

        try {
            val d = Dialog(activity)
            d.requestWindowFeature(Window.FEATURE_NO_TITLE)
            d.setContentView(R.layout.dialog_ad_loading)
            d.setCancelable(false)
            d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            d.show()
            dialog = d
            Log.d(TAG, "AdLoadingDialog: Displaying full-screen ad loading overlay")

            val runnable = Runnable {
                if (dialog?.isShowing == true) {
                    Log.d(TAG, "AdLoadingDialog: Safety timeout ($timeoutMs ms) triggered -> dismissing overlay")
                    dismiss()
                    onTimeout?.invoke()
                }
            }
            timeoutRunnable = runnable
            mainHandler.postDelayed(runnable, timeoutMs)
        } catch (e: Exception) {
            Log.e(TAG, "AdLoadingDialog: Failed to show dialog", e)
            dialog = null
        }
    }

    fun dismiss() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        try {
            if (dialog?.isShowing == true && !activity.isFinishing && !activity.isDestroyed) {
                dialog?.dismiss()
                Log.d(TAG, "AdLoadingDialog: Dismissed loader overlay")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdLoadingDialog: Error dismissing dialog", e)
        } finally {
            dialog = null
        }
    }
}
