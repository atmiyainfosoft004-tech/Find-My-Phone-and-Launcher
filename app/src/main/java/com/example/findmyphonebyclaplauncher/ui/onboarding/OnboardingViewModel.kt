package com.example.findmyphonebyclaplauncher.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.data.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsDataSource  = UserPreferencesDataSource(application)
    private val prefsRepository  = UserPreferencesRepository(prefsDataSource)

    /** Set to `true` when onboarding is fully complete — triggers navigation. */
    val onboardingComplete = MutableLiveData<Boolean>(false)

    fun completeOnboarding(clapEnabled: Boolean = true, whistleEnabled: Boolean = true) {
        viewModelScope.launch {
            prefsRepository.setOnboardingCompleted(true)
            prefsRepository.setClapDetectionEnabled(clapEnabled)
            prefsRepository.setWhistleDetectionEnabled(whistleEnabled)
            prefsRepository.setFindPhoneEnabled(clapEnabled || whistleEnabled)
            onboardingComplete.postValue(true)
        }
    }
}
