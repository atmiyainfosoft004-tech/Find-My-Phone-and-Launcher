package com.example.findmyphonebyclaplauncher.ui.launcher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.data.repository.AppRepository
import com.example.findmyphonebyclaplauncher.domain.usecase.GetDockAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.GetInstalledAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.LaunchAppUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.OpenAppInfoUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val repo = AppRepository.get()
    private val getInstalledApps = GetInstalledAppsUseCase(repo)
    private val getDockApps = GetDockAppsUseCase(repo)
    private val launchApp = LaunchAppUseCase(repo)
    private val toggleFavorite = ToggleFavoriteUseCase(repo)
    private val openAppInfo = OpenAppInfoUseCase(repo)

    val workspaceApps: StateFlow<List<AppInfo>> = getInstalledApps()
        .map { pickWorkspace(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dockApps: StateFlow<List<AppInfo>> = getDockApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { getInstalledApps.refresh() }
    }

    fun openApp(app: AppInfo) = launchApp(app)

    fun onToggleFavorite(app: AppInfo) {
        viewModelScope.launch { toggleFavorite(app) }
    }

    fun onOpenAppInfo(app: AppInfo) = openAppInfo(app)

    fun refreshApps() {
        viewModelScope.launch { getInstalledApps.refresh() }
    }

    fun currentTime(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())

    fun currentDate(): String =
        SimpleDateFormat("EEE dd MMMM", Locale.getDefault()).format(Date())

    private fun pickWorkspace(apps: List<AppInfo>): List<AppInfo> {
        val keys = listOf("clock", "calculator", "calendar", "files", "gallery", "documents")
        val filtered = apps.filter { !it.packageName.contains("findmyphone", ignoreCase = true) }
        val picked = keys.mapNotNull { key ->
            filtered.firstOrNull {
                it.label.contains(key, true) || it.packageName.contains(key, true)
            }
        }.distinctBy { it.packageName }
        return if (picked.size >= 4) picked.take(4) else (picked + filtered).distinctBy { it.packageName }.take(4)
    }
}
