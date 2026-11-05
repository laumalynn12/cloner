package io.github.cloner.manager

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.util.Log
import io.github.cloner.dpc.DpcManager
import io.github.cloner.ipc.ClonerIpcClient

/**
 * Mengelola status pembekuan (Freeze/Unfreeze) dan peluncuran aplikasi.
 */
class AppFreezeManager(
    private val context: Context,
    private val dpcManager: DpcManager,
    private val ipcClient: ClonerIpcClient
) {
    private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    fun isAppFrozen(packageName: String, userHandle: UserHandle): Boolean {
        val myUser = Process.myUserHandle()
        return if (myUser == userHandle) {
            dpcManager.isApplicationHidden(packageName)
        } else {
            val userId = getUserId(userHandle)
            ipcClient.isAppFrozen(userId, packageName)
        }
    }

    fun setAppFrozen(packageName: String, userHandle: UserHandle, freeze: Boolean): Boolean {
        val myUser = Process.myUserHandle()
        return if (myUser == userHandle) {
            dpcManager.setApplicationHidden(packageName, freeze)
        } else {
            val userId = getUserId(userHandle)
            if (freeze) {
                ipcClient.freezeApp(userId, packageName)
            } else {
                ipcClient.unfreezeApp(userId, packageName)
            }
        }
    }

    fun launchApp(packageName: String, userHandle: UserHandle): Boolean {
        return try {
            // Unfreeze terlebih dahulu jika sedang beku
            if (isAppFrozen(packageName, userHandle)) {
                setAppFrozen(packageName, userHandle, false)
            }

            val activities = launcherApps.getActivityList(packageName, userHandle)
            if (activities.isNotEmpty()) {
                val component = activities[0].componentName
                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } else {
                Log.w(TAG, "Tidak ditemukan launcher activity untuk $packageName pada $userHandle")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal meluncurkan aplikasi $packageName", e)
            false
        }
    }

    private fun getUserId(userHandle: UserHandle): Int {
        return userHandle.hashCode().let { it % 100000 }
    }

    companion object {
        private const val TAG = "Cloner.FreezeMgr"
    }
}
