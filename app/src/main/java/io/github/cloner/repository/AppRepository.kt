package io.github.cloner.repository

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.util.Log
import io.github.cloner.dpc.ClonerDeviceAdminReceiver
import io.github.cloner.dpc.DpcManager
import io.github.cloner.manager.AppFreezeManager
import io.github.cloner.manager.QuietModeManager
import io.github.cloner.model.AppEntry
import io.github.cloner.model.ProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository data aplikasi dan status profil.
 * Menggunakan LauncherApps API resmi untuk query aplikasi personal dan work profile.
 */
class AppRepository(
    private val context: Context,
    private val dpcManager: DpcManager,
    private val freezeManager: AppFreezeManager,
    private val quietModeManager: QuietModeManager
) {
    private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val _personalApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val personalApps: StateFlow<List<AppEntry>> = _personalApps.asStateFlow()

    private val _workApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val workApps: StateFlow<List<AppEntry>> = _workApps.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.NotCreated)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        val myUser = Process.myUserHandle()
        val allProfiles = launcherApps.profiles

        val workProfile = allProfiles.find { it != myUser }
        val currentProfileState = when {
            workProfile == null -> ProfileState.NotCreated
            quietModeManager.isQuietModeEnabled(workProfile) -> ProfileState.Quiet(workProfile)
            else -> ProfileState.Active(workProfile)
        }
        _profileState.value = currentProfileState

        // 1. Muat aplikasi personal
        val personalList = loadAppsForProfile(myUser)
        _personalApps.value = personalList

        // 2. Muat aplikasi work profile jika tersedia
        if (workProfile != null && currentProfileState !is ProfileState.Quiet) {
            val workList = loadAppsForProfile(workProfile)
            _workApps.value = workList
        } else {
            _workApps.value = emptyList()
        }
    }

    private fun loadAppsForProfile(userHandle: UserHandle): List<AppEntry> {
        return try {
            val activities = launcherApps.getActivityList(null, userHandle)
            activities.map { activityInfo ->
                val appInfo = activityInfo.applicationInfo
                val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isFrozen = freezeManager.isAppFrozen(activityInfo.applicationInfo.packageName, userHandle)

                AppEntry(
                    packageName = activityInfo.applicationInfo.packageName,
                    label = activityInfo.label.toString(),
                    icon = activityInfo.getBadgedIcon(0),
                    isSystem = isSys,
                    isFrozen = isFrozen,
                    userHandle = userHandle
                )
            }.sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat aplikasi untuk user $userHandle", e)
            emptyList()
        }
    }

    fun startManagedProfileProvisioning(activity: Activity, requestCode: Int) {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                dpcManager.adminComponent
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }

    companion object {
        private const val TAG = "Cloner.AppRepo"
    }
}
