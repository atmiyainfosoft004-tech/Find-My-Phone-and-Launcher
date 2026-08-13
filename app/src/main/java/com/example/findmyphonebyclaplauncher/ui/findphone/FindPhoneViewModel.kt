package com.example.findmyphonebyclaplauncher.ui.findphone

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import com.example.findmyphonebyclaplauncher.data.repository.UserPreferencesRepository
import com.example.findmyphonebyclaplauncher.service.SoundDetectionService
import com.example.findmyphonebyclaplauncher.utils.PermissionManager
import kotlinx.coroutines.launch

class FindPhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsDataSource = UserPreferencesDataSource(application)
    private val prefsRepository = UserPreferencesRepository(prefsDataSource)

    val isClapEnabled          = MutableLiveData<Boolean>()
    val isWhistleEnabled       = MutableLiveData<Boolean>()
    val isServiceRunning       = MutableLiveData<Boolean>()
    val hasMicPermission       = MutableLiveData<Boolean>()

    val isSoundAlertEnabled    = MutableLiveData<Boolean>()
    val isFlashlightEnabled    = MutableLiveData<Boolean>()
    val isVibrationEnabled     = MutableLiveData<Boolean>()
    val selectedAlertSound     = MutableLiveData<String>()
    val selectedAlertDuration  = MutableLiveData<Int>()

    init {
        refreshState()
    }

    fun refreshState() {
        val clap    = prefsDataSource.isClapDetectionEnabled
        val whistle = prefsDataSource.isWhistleDetectionEnabled
        isClapEnabled.value          = clap
        isWhistleEnabled.value       = whistle
        isServiceRunning.value       = clap || whistle
        hasMicPermission.value       = PermissionManager.hasRecordAudioPermission(getApplication())

        isSoundAlertEnabled.value    = prefsDataSource.isSoundAlertEnabled
        isFlashlightEnabled.value    = prefsDataSource.isFlashlightEnabled
        isVibrationEnabled.value     = prefsDataSource.isVibrationEnabled
        selectedAlertSound.value     = prefsDataSource.selectedAlertSound
        selectedAlertDuration.value  = prefsDataSource.selectedAlertDuration
    }

    fun setMasterDetection(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setClapDetectionEnabled(enabled)
            prefsRepository.setWhistleDetectionEnabled(enabled)
            prefsRepository.setFindPhoneEnabled(enabled)
            isClapEnabled.postValue(enabled)
            isWhistleEnabled.postValue(enabled)
            updateService(context, enabled, enabled)
        }
    }

    fun setClapDetection(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setClapDetectionEnabled(enabled)
            if (enabled) prefsRepository.setFindPhoneEnabled(true)
            isClapEnabled.postValue(enabled)
            updateService(context, enabled, prefsDataSource.isWhistleDetectionEnabled)
        }
    }

    fun setWhistleDetection(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setWhistleDetectionEnabled(enabled)
            if (enabled) prefsRepository.setFindPhoneEnabled(true)
            isWhistleEnabled.postValue(enabled)
            updateService(context, prefsDataSource.isClapDetectionEnabled, enabled)
        }
    }

    fun setSoundAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setSoundAlertEnabled(enabled)
            isSoundAlertEnabled.postValue(enabled)
        }
    }

    fun setFlashlightEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setFlashlightEnabled(enabled)
            isFlashlightEnabled.postValue(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setVibrationEnabled(enabled)
            isVibrationEnabled.postValue(enabled)
        }
    }

    fun setSelectedAlertSound(sound: String) {
        viewModelScope.launch {
            prefsRepository.setSelectedAlertSound(sound)
            selectedAlertSound.postValue(sound)
        }
    }

    fun setSelectedAlertDuration(durationSeconds: Int) {
        viewModelScope.launch {
            prefsRepository.setSelectedAlertDuration(durationSeconds)
            selectedAlertDuration.postValue(durationSeconds)
        }
    }

    private fun updateService(context: Context, clap: Boolean, whistle: Boolean) {
        val anyEnabled = clap || whistle
        isServiceRunning.postValue(anyEnabled)

        if (!anyEnabled) {
            context.stopService(SoundDetectionService.stopIntent(context))
            return
        }

        val intent = SoundDetectionService.startIntent(context, clap, whistle)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
