package com.example.findmyphonebyclaplauncher.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.data.repository.AppRepository
import com.example.findmyphonebyclaplauncher.domain.usecase.GetDockAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.GetInstalledAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.LaunchAppUseCase
import com.example.findmyphonebyclaplauncher.ui.launcher.adapter.LauncherPagerAdapter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel : ViewModel() {

    private val repo = AppRepository.get()
    private val getInstalledApps = GetInstalledAppsUseCase(repo)
    private val getDockApps = GetDockAppsUseCase(repo)
    private val launchApp = LaunchAppUseCase(repo)

    private val _currentPage = MutableStateFlow(LauncherPagerAdapter.PAGE_HOME)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    private val _openDrawerEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openDrawerEvents: SharedFlow<Unit> = _openDrawerEvents.asSharedFlow()

    private val _closeDrawerEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeDrawerEvents: SharedFlow<Unit> = _closeDrawerEvents.asSharedFlow()

    private val _focusDrawerSearchEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusDrawerSearchEvents: SharedFlow<Unit> = _focusDrawerSearchEvents.asSharedFlow()

    private var openDrawerWithSearch = false

    val dockApps: StateFlow<List<AppInfo>> = getDockApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { getInstalledApps.refresh() }
    }

    fun setPage(page: Int) {
        _currentPage.value = page
    }

    fun openApp(app: AppInfo) = launchApp(app)

    fun requestOpenDrawer() {
        openDrawerWithSearch = false
        _openDrawerEvents.tryEmit(Unit)
    }

    fun requestOpenDrawerWithSearch() {
        if (_drawerOpen.value) {
            openDrawerWithSearch = false
            _focusDrawerSearchEvents.tryEmit(Unit)
            return
        }
        openDrawerWithSearch = true
        _openDrawerEvents.tryEmit(Unit)
    }

    fun requestCloseDrawer() {
        openDrawerWithSearch = false
        _closeDrawerEvents.tryEmit(Unit)
    }

    fun setDrawerOpenState(open: Boolean) {
        _drawerOpen.value = open
        if (open && openDrawerWithSearch) {
            openDrawerWithSearch = false
            _focusDrawerSearchEvents.tryEmit(Unit)
        }
        if (!open) {
            openDrawerWithSearch = false
        }
    }
}
