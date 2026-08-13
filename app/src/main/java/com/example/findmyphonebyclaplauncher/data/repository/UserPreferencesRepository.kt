package com.example.findmyphonebyclaplauncher.data.repository

import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that wraps [UserPreferencesDataSource] and exposes suspend and synchronous getters/setters.
 * ViewModels interact with preferences through this layer.
 */
class UserPreferencesRepository(private val dataSource: UserPreferencesDataSource) {

    suspend fun isOnboardingCompleted(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isOnboardingCompleted
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isOnboardingCompleted = completed
    }

    suspend fun isClapDetectionEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isClapDetectionEnabled
    }

    suspend fun setClapDetectionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isClapDetectionEnabled = enabled
    }

    suspend fun isWhistleDetectionEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isWhistleDetectionEnabled
    }

    suspend fun setWhistleDetectionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isWhistleDetectionEnabled = enabled
    }

    suspend fun isFindPhoneEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isFindPhoneEnabled
    }

    suspend fun setFindPhoneEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isFindPhoneEnabled = enabled
    }

    fun isOnboardingCompletedSync(): Boolean = dataSource.isOnboardingCompleted
    fun isClapDetectionEnabledSync(): Boolean = dataSource.isClapDetectionEnabled
    fun isWhistleDetectionEnabledSync(): Boolean = dataSource.isWhistleDetectionEnabled

    suspend fun isSoundAlertEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isSoundAlertEnabled
    }

    suspend fun setSoundAlertEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isSoundAlertEnabled = enabled
    }

    suspend fun isFlashlightEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isFlashlightEnabled
    }

    suspend fun setFlashlightEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isFlashlightEnabled = enabled
    }

    suspend fun isVibrationEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataSource.isVibrationEnabled
    }

    suspend fun setVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataSource.isVibrationEnabled = enabled
    }

    suspend fun getSelectedAlertSound(): String = withContext(Dispatchers.IO) {
        dataSource.selectedAlertSound
    }

    suspend fun setSelectedAlertSound(sound: String) = withContext(Dispatchers.IO) {
        dataSource.selectedAlertSound = sound
    }

    suspend fun getSelectedAlertDuration(): Int = withContext(Dispatchers.IO) {
        dataSource.selectedAlertDuration
    }

    suspend fun setSelectedAlertDuration(duration: Int) = withContext(Dispatchers.IO) {
        dataSource.selectedAlertDuration = duration
    }
}
