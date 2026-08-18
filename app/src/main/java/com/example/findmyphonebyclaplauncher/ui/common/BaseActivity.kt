package com.example.findmyphonebyclaplauncher.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.findmyphonebyclaplauncher.utils.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.onAttach(newBase)
        super.attachBaseContext(contextWithLocale)
    }
}
