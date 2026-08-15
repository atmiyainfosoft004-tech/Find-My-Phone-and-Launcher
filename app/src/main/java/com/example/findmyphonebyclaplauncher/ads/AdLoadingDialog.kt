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
            Log.d("InterstitialDebug", "AdLoadingDialog: Displaying loader overlay")

            val runnable = Runnable {
                if (dialog?.isShowing == true) {
                    Log.d("InterstitialDebug", "AdLoadingDialog: Safety timeout ($timeoutMs ms) triggered")
                    dismiss()
                    onTimeout?.invoke()
                }
            }
            timeoutRunnable = runnable
            mainHandler.postDelayed(runnable, timeoutMs)
        } catch (e: Exception) {
            Log.e("InterstitialDebug", "AdLoadingDialog: Failed to show dialog", e)
            dialog = null
        }
    }

    fun dismiss() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        try {
            if (dialog?.isShowing == true && !activity.isFinishing && !activity.isDestroyed) {
                dialog?.dismiss()
                Log.d("InterstitialDebug", "AdLoadingDialog: Dismissed loader overlay")
            }
        } catch (e: Exception) {
            Log.e("InterstitialDebug", "AdLoadingDialog: Error dismissing dialog", e)
        } finally {
            dialog = null
        }
    }
}
