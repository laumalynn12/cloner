package io.github.cloner.manager

import android.content.Context
import android.os.UserHandle
import android.util.Log
import io.github.cloner.ipc.ClonerIpcClient

/**
 * Mengelola kloning aplikasi dari profil utama ke profil kerja terisolasi.
 * Menggunakan API installExistingPackage untuk efisiensi penyimpanan tanpa duplikasi APK fisik.
 */
class AppCloneManager(
    private val context: Context,
    private val ipcClient: ClonerIpcClient
) {

    fun cloneToProfile(packageName: String, targetProfile: UserHandle): Boolean {
        val targetUserId = targetProfile.hashCode().let { it % 100000 }
        Log.i(TAG, "Mencoba kloning $packageName ke user $targetUserId...")

        val success = ipcClient.installExisting(targetUserId, packageName)
        if (success) {
            Log.i(TAG, "Kloning $packageName ke user $targetUserId berhasil.")
        } else {
            Log.w(TAG, "Kloning $packageName ke user $targetUserId gagal.")
        }
        return success
    }

    companion object {
        private const val TAG = "Cloner.CloneMgr"
    }
}
