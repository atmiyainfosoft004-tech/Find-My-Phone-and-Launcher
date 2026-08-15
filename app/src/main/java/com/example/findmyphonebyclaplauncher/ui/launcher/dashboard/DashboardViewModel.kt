package com.example.findmyphonebyclaplauncher.ui.launcher.dashboard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.data.repository.AppRepository
import com.example.findmyphonebyclaplauncher.domain.usecase.GetRecentAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.GetSuggestedAppsUseCase
import com.example.findmyphonebyclaplauncher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UsageCardState(
    val hasPermission: Boolean = false,
    val totalLabel: String = "",
    val topLines: List<String> = emptyList()
)

class DashboardViewModel : ViewModel() {

    private val repo = AppRepository.get()
    private val getSuggestedApps = GetSuggestedAppsUseCase(repo)
    private val getRecentApps = GetRecentAppsUseCase(repo)
    private val launchApp = LaunchAppUseCase(repo)

    val suggestedApps: StateFlow<List<AppInfo>> = getSuggestedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentApps: StateFlow<List<AppInfo>> = getRecentApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _usageCard = MutableStateFlow(UsageCardState())
    val usageCard: StateFlow<UsageCardState> = _usageCard.asStateFlow()

    fun openApp(app: AppInfo) = launchApp(app)

    fun refreshUsageCard(context: Context) {
        viewModelScope.launch {
            val hasPermission = checkUsagePermission(context)
            _usageCard.value = UsageCardState(
                hasPermission = hasPermission,
                totalLabel = if (hasPermission) "Active today" else "Usage permission required",
                topLines = if (hasPermission) listOf("Apps ready for launch") else emptyList()
            )
        }
    }

    fun openUsageAccessSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun checkUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
