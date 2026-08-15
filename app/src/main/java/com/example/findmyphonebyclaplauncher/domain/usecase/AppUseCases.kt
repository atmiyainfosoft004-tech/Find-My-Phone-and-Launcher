package com.example.findmyphonebyclaplauncher.domain.usecase

import com.example.findmyphonebyclaplauncher.data.model.AppCategory
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.data.repository.AppRepositoryImpl
import kotlinx.coroutines.flow.Flow

class GetInstalledAppsUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.apps
    suspend fun refresh() = repository.refreshApps()
}

class SearchAppsUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(query: String): Flow<List<AppInfo>> = repository.searchApps(query)
}

class GetAppsByCategoryUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(category: AppCategory): Flow<List<AppInfo>> = repository.appsByCategory(category)
}

class LaunchAppUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(app: AppInfo) = repository.launchApp(app)
}

class ToggleFavoriteUseCase(private val repository: AppRepositoryImpl) {
    suspend operator fun invoke(app: AppInfo) = repository.toggleFavorite(app)
}

class OpenAppInfoUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(app: AppInfo) = repository.openAppInfo(app)
}

class UninstallAppUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(app: AppInfo): Boolean = repository.uninstallApp(app)
}

class ObserveHasFavoritesUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(): Flow<Boolean> = repository.hasFavorites
}

class GetDockAppsUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.getDockApps()
}

class GetSuggestedAppsUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.getSuggestedApps()
}

class GetRecentAppsUseCase(private val repository: AppRepositoryImpl) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.getRecentApps()
}
