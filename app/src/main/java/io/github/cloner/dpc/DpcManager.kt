package io.github.cloner.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * Pengelola API resmi Android Enterprise DevicePolicyManager.
 * Semua panggilan menggunakan API publik murni tanpa refleksi/hidden API.
 */
class DpcManager(private val context: Context) {

    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    val adminComponent: ComponentName by lazy {
        ComponentName(context, ClonerDeviceAdminReceiver::class.java)
    }

    val isProfileOwner: Boolean
        get() = dpm.isProfileOwnerApp(context.packageName)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    fun isProvisioningAllowed(): Boolean {
        return try {
            dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
        } catch (e: Exception) {
            true
        }
    }

    fun setApplicationHidden(packageName: String, hidden: Boolean): Boolean {
        return try {
            dpm.setApplicationHidden(adminComponent, packageName, hidden)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengubah status hidden untuk: $packageName", e)
            false
        }
    }

    fun isApplicationHidden(packageName: String): Boolean {
        return try {
            dpm.isApplicationHidden(adminComponent, packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun installExistingPackage(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.installExistingPackage(adminComponent, packageName)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal installExistingPackage untuk: $packageName", e)
            false
        }
    }

    fun enableSystemApp(packageName: String): Boolean {
        return try {
            dpm.enableSystemApp(adminComponent, packageName)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Paket sistem tidak ditemukan untuk di-enable: $packageName")
            false
        }
    }

    fun setProfileEnabled() {
        try {
            dpm.setProfileEnabled(adminComponent)
            Log.i(TAG, "Work Profile berhasil diaktifkan.")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengaktifkan profil", e)
        }
    }

    fun setAffiliationIds(affiliationIds: Set<String>) {
        try {
            dpm.setAffiliationIds(adminComponent, affiliationIds)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menetapkan affiliation IDs", e)
        }
    }

    fun addCrossProfileIntentFilter(filter: IntentFilter, flags: Int) {
        try {
            dpm.addCrossProfileIntentFilter(adminComponent, filter, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menambahkan cross-profile intent filter", e)
        }
    }

    fun clearCrossProfileIntentFilters() {
        try {
            dpm.clearCrossProfileIntentFilters(adminComponent)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membersihkan cross-profile intent filter", e)
        }
    }

    fun setProfileName(name: String) {
        try {
            dpm.setProfileName(adminComponent, name)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menetapkan nama profil", e)
        }
    }

    companion object {
        private const val TAG = "Cloner.DPC"
        const val AFFILIATION_ID = "io.github.cloner.affiliation"
    }
}
