package io.github.cloner.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log

/**
 * Mengelola status Quiet Mode (profil senyap) untuk mematikan seluruh proses
 * di dalam profil kerja saat tidak digunakan, mencapai 0% konsumsi CPU & RAM.
 */
class QuietModeManager(private val context: Context) {

    private val userManager: UserManager by lazy {
        context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    val hasQuietModePermission: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.checkSelfPermission("android.permission.MODIFY_QUIET_MODE") == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }

    fun isQuietModeEnabled(userHandle: UserHandle): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                userManager.isQuietModeEnabled(userHandle)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Meminta pengaktifan atau penonaktifan quiet mode.
     * Mengembalikan true jika permintaan berhasil dieksekusi.
     */
    fun requestQuietMode(enable: Boolean, userHandle: UserHandle): QuietModeResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return QuietModeResult.Unsupported("Fitur Quiet Mode membutuhkan minimal Android 9 (API 28).")
        }

        return try {
            val accepted = userManager.requestQuietModeEnabled(enable, userHandle)
            if (accepted) {
                Log.i(TAG, "Permintaan Quiet Mode ($enable) untuk $userHandle diterima sistem.")
                QuietModeResult.Success
            } else {
                Log.w(TAG, "Permintaan Quiet Mode ditolak oleh sistem.")
                QuietModeResult.Rejected
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException saat memanggil requestQuietModeEnabled", e)
            QuietModeResult.PermissionRequired(
                "Izin MODIFY_QUIET_MODE diperlukan. Berikan via ADB:\n" +
                "adb shell pm grant ${context.packageName} android.permission.MODIFY_QUIET_MODE"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengubah quiet mode", e)
            QuietModeResult.Error(e.message ?: "Kesalahan tak terduga")
        }
    }

    fun openSystemSyncSettings() {
        try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka pengaturan sinkronisasi akun", e)
        }
    }

    companion object {
        private const val TAG = "Cloner.QuietMode"
    }
}

sealed interface QuietModeResult {
    data object Success : QuietModeResult
    data object Rejected : QuietModeResult
    data class PermissionRequired(val instructions: String) : QuietModeResult
    data class Unsupported(val reason: String) : QuietModeResult
    data class Error(val message: String) : QuietModeResult
}
