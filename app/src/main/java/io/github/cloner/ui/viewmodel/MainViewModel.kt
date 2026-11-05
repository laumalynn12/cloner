package io.github.cloner.ui.viewmodel

import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.cloner.manager.AppCloneManager
import io.github.cloner.manager.AppFreezeManager
import io.github.cloner.manager.QuietModeManager
import io.github.cloner.manager.QuietModeResult
import io.github.cloner.model.AppEntry
import io.github.cloner.model.ProfileState
import io.github.cloner.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppRepository,
    private val freezeManager: AppFreezeManager,
    private val cloneManager: AppCloneManager,
    private val quietModeManager: QuietModeManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = Mainland (Personal), 1 = Sandbox (Work)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showSetupGuide = MutableStateFlow(false)
    val showSetupGuide: StateFlow<Boolean> = _showSetupGuide.asStateFlow()

    private val _guideMessage = MutableStateFlow("")
    val guideMessage: StateFlow<String> = _guideMessage.asStateFlow()

    val profileState: StateFlow<ProfileState> = repository.profileState

    val displayedApps: StateFlow<List<AppEntry>> = combine(
        repository.personalApps,
        repository.workApps,
        _selectedTab,
        _searchQuery
    ) { personal, work, tab, query ->
        val rawList = if (tab == 0) personal else work
        if (query.isBlank()) {
            rawList
        } else {
            rawList.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAll()
            _isRefreshing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun launchApp(app: AppEntry) {
        viewModelScope.launch {
            freezeManager.launchApp(app.packageName, app.userHandle)
            refresh()
        }
    }

    fun toggleFreeze(app: AppEntry) {
        viewModelScope.launch {
            val targetFreeze = !app.isFrozen
            freezeManager.setAppFrozen(app.packageName, app.userHandle, targetFreeze)
            refresh()
        }
    }

    fun cloneApp(app: AppEntry, targetProfile: UserHandle) {
        viewModelScope.launch {
            cloneManager.cloneToProfile(app.packageName, targetProfile)
            refresh()
        }
    }

    fun toggleQuietMode(targetProfile: UserHandle) {
        val currentlyQuiet = quietModeManager.isQuietModeEnabled(targetProfile)
        val result = quietModeManager.requestQuietMode(!currentlyQuiet, targetProfile)

        when (result) {
            is QuietModeResult.Success -> {
                refresh()
            }
            is QuietModeResult.PermissionRequired -> {
                _guideMessage.value = result.instructions
                _showSetupGuide.value = true
            }
            is QuietModeResult.Unsupported -> {
                _guideMessage.value = result.reason
                _showSetupGuide.value = true
            }
            is QuietModeResult.Rejected, is QuietModeResult.Error -> {
                quietModeManager.openSystemSyncSettings()
            }
        }
    }

    fun showAdbProvisioningGuide() {
        _guideMessage.value =
            "Jika perangkat Anda (seperti MIUI/HyperOS) memblokir pembuatan profil lewat UI, jalankan perintah ADB berikut:\n\n" +
            "1. Buat profil kerja:\n" +
            "adb shell pm create-user --profileOf 0 --managed ClonerProfile\n\n" +
            "2. Catat ID user yang muncul (misal 10), lalu pasang Cloner ke user tersebut:\n" +
            "adb shell pm install-existing --user 10 io.github.cloner\n\n" +
            "3. Jadikan Cloner sebagai Profile Owner:\n" +
            "adb shell dpm set-profile-owner --user 10 io.github.cloner/.dpc.ClonerDeviceAdminReceiver\n\n" +
            "4. Aktifkan user profil:\n" +
            "adb shell am start-user 10"
        _showSetupGuide.value = true
    }

    fun dismissSetupGuide() {
        _showSetupGuide.value = false
    }

    class Factory(
        private val repository: AppRepository,
        private val freezeManager: AppFreezeManager,
        private val cloneManager: AppCloneManager,
        private val quietModeManager: QuietModeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, freezeManager, cloneManager, quietModeManager) as T
        }
    }
}
