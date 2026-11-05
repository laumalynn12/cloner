package io.github.cloner.provisioning

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView

/**
 * Menyelesaikan paket sistem penting secara dinamis berdasarkan Android Framework & OEM.
 * Menghindari hardcode yang rentan patah antar-versi Android atau antar-vendor HP.
 */
class DynamicWhitelistResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Mengumpulkan semua paket yang mutlak dibutuhkan agar sistem, Play Store, dan GMS stabil.
     */
    fun resolveEssentialPackages(): Set<String> {
        val essential = mutableSetOf<String>()

        // 1. Fondasi Inti Google & Android
        essential.add("android")
        essential.add(context.packageName) // Aplikasi Cloner sendiri
        essential.add(PKG_PLAY_STORE)
        essential.add(PKG_PLAY_SERVICES)
        essential.add(PKG_GSF)
        essential.add("com.android.settings")
        essential.add("com.android.keychain")
        essential.add("com.android.providers.downloads")
        essential.add("com.android.providers.downloads.ui")
        essential.add("com.android.documentsui")
        essential.add("com.android.vpndialogs")

        // 2. Resolusi Dinamis: Package Installer
        resolvePackageInstaller()?.let { essential.add(it) }

        // 3. Resolusi Dinamis: Permission Controller (Android 10+)
        resolvePermissionController()?.let { essential.add(it) }

        // 4. Resolusi Dinamis: WebView Provider Aktif
        resolveWebViewPackage()?.let { essential.add(it) }

        // 5. Resolusi Dinamis: Media Provider
        resolveMediaProvider()?.let { essential.add(it) }

        Log.i(TAG, "Total paket sistem esensial terdeteksi: ${essential.size} paket")
        return essential
    }

    private fun resolvePackageInstaller(): String? {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = Uri.fromParts("package", "dummy.package", null)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName?.also {
            Log.d(TAG, "Terdeteksi Package Installer: $it")
        }
    }

    private fun resolvePermissionController(): String? {
        val intent = Intent("android.content.pm.action.REQUEST_PERMISSIONS")
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName?.also {
            Log.d(TAG, "Terdeteksi Permission Controller: $it")
        }
    }

    private fun resolveWebViewPackage(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WebView.getCurrentWebViewPackage()?.packageName?.also {
                    Log.d(TAG, "Terdeteksi Active WebView: $it")
                }
            } else {
                "com.google.android.webview"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mendeteksi WebView aktif, menggunakan default", e)
            "com.google.android.webview"
        }
    }

    private fun resolveMediaProvider(): String? {
        val providerInfo = packageManager.resolveContentProvider(MediaStore.AUTHORITY, 0)
        return providerInfo?.packageName?.also {
            Log.d(TAG, "Terdeteksi Media Provider: $it")
        }
    }

    companion object {
        private const val TAG = "Cloner.Whitelist"
        const val PKG_PLAY_STORE = "com.android.vending"
        const val PKG_PLAY_SERVICES = "com.google.android.gms"
        const val PKG_GSF = "com.google.android.gsf"
    }
}
