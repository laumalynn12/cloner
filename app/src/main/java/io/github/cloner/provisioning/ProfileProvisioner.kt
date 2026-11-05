package io.github.cloner.provisioning

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import io.github.cloner.dpc.DpcManager

/**
 * Orkestrator proses pasca-provisioning profil kerja terisolasi.
 * Bertanggung jawab membersihkan bloatware secara dinamis dan hanya menyisakan Play Store + fondasi.
 */
class ProfileProvisioner(
    private val context: Context,
    private val dpcManager: DpcManager,
    private val whitelistResolver: DynamicWhitelistResolver
) {

    fun performPostProvisioning() {
        Log.i(TAG, "Memulai inisialisasi profil kerja baru...")

        // 1. Konfigurasi Afiliasi (krusial untuk installExistingPackage di Android 9+)
        dpcManager.setAffiliationIds(setOf(DpcManager.AFFILIATION_ID))
        dpcManager.setProfileName("Cloner Sandbox")

        // 2. Pasang Cross-Profile Intent Filter Standar
        setupCrossProfileFilters()

        // 3. Pastikan Play Store dan GMS Aktif
        ensureCoreGooglePackages()

        // 4. Jalankan Debloat Otomatis
        executeDynamicDebloat()

        // 5. Aktifkan Profil Kerja agar aplikasi di dalamnya siap digunakan
        dpcManager.setProfileEnabled()
        Log.i(TAG, "Inisialisasi profil kerja selesai dengan sukses.")
    }

    private fun setupCrossProfileFilters() {
        dpcManager.clearCrossProfileIntentFilters()

        // Teruskan link URL dari sandbox ke browser profil utama (mengeliminasi kebutuhan browser kedua)
        val viewWebFilter = IntentFilter(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addDataScheme("http")
            addDataScheme("https")
        }
        dpcManager.addCrossProfileIntentFilter(
            viewWebFilter,
            DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED or DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT
        )

        // Teruskan dialer & SMS
        val dialFilter = IntentFilter(Intent.ACTION_DIAL).apply {
            addDataScheme("tel")
        }
        dpcManager.addCrossProfileIntentFilter(dialFilter, DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT)
    }

    private fun ensureCoreGooglePackages() {
        dpcManager.enableSystemApp(DynamicWhitelistResolver.PKG_PLAY_STORE)
        dpcManager.enableSystemApp(DynamicWhitelistResolver.PKG_PLAY_SERVICES)
        dpcManager.enableSystemApp(DynamicWhitelistResolver.PKG_GSF)

        dpcManager.setApplicationHidden(DynamicWhitelistResolver.PKG_PLAY_STORE, false)
        dpcManager.setApplicationHidden(DynamicWhitelistResolver.PKG_PLAY_SERVICES, false)
    }

    fun executeDynamicDebloat(): Int {
        val pm = context.packageManager
        val essentialPackages = whitelistResolver.resolveEssentialPackages()
        val installedPackages = pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)

        var hiddenCount = 0
        Log.d(TAG, "Memulai audit profil: ${installedPackages.size} paket terpasang saat ini.")

        for (pkgInfo in installedPackages) {
            val pkg = pkgInfo.packageName

            // Jangan sentuh paket esensial
            if (essentialPackages.contains(pkg)) {
                continue
            }

            val appInfo = pkgInfo.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Sembunyikan semua bloatware sistem yang memiliki launcher atau bukan bagian inti
            if (isSystem) {
                val hasLauncher = pm.getLaunchIntentForPackage(pkg) != null
                if (hasLauncher || isKnownBloatware(pkg)) {
                    val success = dpcManager.setApplicationHidden(pkg, true)
                    if (success) {
                        hiddenCount++
                        Log.d(TAG, "Debloated: $pkg")
                    }
                }
            }
        }

        Log.i(TAG, "Pembersihan profil selesai: $hiddenCount bloatware disembunyikan.")
        return hiddenCount
    }

    private fun isKnownBloatware(packageName: String): Boolean {
        val bloatPrefixes = listOf(
            "com.miui.", "com.xiaomi.", "com.samsung.", "com.sec.android.",
            "com.oppo.", "com.coloros.", "com.vivo.", "com.huawei.",
            "com.google.android.youtube", "com.google.android.apps.docs",
            "com.google.android.apps.photos", "com.google.android.apps.maps",
            "com.google.android.googlequicksearchbox"
        )
        return bloatPrefixes.any { packageName.startsWith(it) }
    }

    companion object {
        private const val TAG = "Cloner.Provisioner"
    }
}
