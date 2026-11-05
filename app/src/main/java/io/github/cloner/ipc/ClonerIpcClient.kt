package io.github.cloner.ipc

import android.content.Context
import android.os.Bundle
import android.util.Log

/**
 * Klien IPC yang dipanggil dari Profil Utama (Personal Profile).
 * Berkomunikasi langsung ke endpoint ClonerCommandProvider di Work Profile tanpa refleksi atau proxy activity.
 */
class ClonerIpcClient(private val context: Context) {

    fun ping(targetUserId: Int): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_PING, null)
    }

    fun freezeApp(targetUserId: Int, packageName: String): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_FREEZE, packageName)
    }

    fun unfreezeApp(targetUserId: Int, packageName: String): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_UNFREEZE, packageName)
    }

    fun isAppFrozen(targetUserId: Int, packageName: String): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_IS_FROZEN, packageName)
    }

    fun installExisting(targetUserId: Int, packageName: String): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_INSTALL_EXISTING, packageName)
    }

    fun triggerDebloat(targetUserId: Int): Boolean {
        return callRemote(targetUserId, ClonerIpcContract.METHOD_TRIGGER_DEBLOAT, null)
    }

    private fun callRemote(targetUserId: Int, method: String, packageName: String?): Boolean {
        return try {
            val targetUri = ClonerIpcContract.buildUserTargetUri(targetUserId)
            val extras = Bundle().apply {
                if (packageName != null) {
                    putString(ClonerIpcContract.EXTRA_PACKAGE_NAME, packageName)
                }
            }

            val result = context.contentResolver.call(targetUri, method, packageName, extras)
            val success = result?.getBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, false) ?: false
            val message = result?.getString(ClonerIpcContract.EXTRA_RESULT_MESSAGE)

            Log.d(TAG, "IPC Result [$method -> User $targetUserId]: success=$success, msg=$message")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memanggil IPC $method ke User $targetUserId", e)
            false
        }
    }

    companion object {
        private const val TAG = "Cloner.IpcClient"
    }
}
