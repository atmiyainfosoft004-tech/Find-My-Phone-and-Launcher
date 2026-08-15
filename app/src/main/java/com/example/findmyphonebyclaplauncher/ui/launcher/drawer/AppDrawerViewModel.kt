package com.example.findmyphonebyclaplauncher.ui.launcher.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.model.AppCategory
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.data.repository.AppRepository
import com.example.findmyphonebyclaplauncher.domain.usecase.GetAppsByCategoryUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.GetInstalledAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.LaunchAppUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.ObserveHasFavoritesUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.OpenAppInfoUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.SearchAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.ToggleFavoriteUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.UninstallAppUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModel : ViewModel() {

    private val repo = AppRepository.get()
    private val searchApps = SearchAppsUseCase(repo)
    private val getAppsByCategory = GetAppsByCategoryUseCase(repo)
    private val launchApp = LaunchAppUseCase(repo)
    private val toggleFavorite = ToggleFavoriteUseCase(repo)
    private val openAppInfo = OpenAppInfoUseCase(repo)
    private val uninstallApp = UninstallAppUseCase(repo)
    private val getInstalledApps = GetInstalledAppsUseCase(repo)
    private val observeHasFavorites = ObserveHasFavoritesUseCase(repo)

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(AppCategory.ALL)

    val hasFavorites: StateFlow<Boolean> = observeHasFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val selectedCategory: StateFlow<AppCategory> = category.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) ->
            if (q.isNotBlank()) searchApps(q)
            else getAppsByCategory(c)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun onCategorySelected(category: AppCategory) {
        this.category.value = category
    }

    fun openApp(app: AppInfo) = launchApp(app)

    fun onToggleFavorite(app: AppInfo) {
        viewModelScope.launch { toggleFavorite(app) }
    }

    fun onOpenAppInfo(app: AppInfo) = openAppInfo(app)

    fun onUninstall(app: AppInfo): Boolean = uninstallApp(app)

    fun refreshApps() {
        viewModelScope.launch { getInstalledApps.refresh() }
    }
}
