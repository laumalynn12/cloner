# Hasil Eksplorasi Mendalam: Arsitektur Island & Deagle vs Desain Island-2

Dokumen ini memetakan temuan teknis dari kode sumber asli `~/island` (`oasisfeng/island`) dan library pendukungnya `~/deagle` (`oasisfeng/deagle`), menganalisis kelemahan arsitektur lama, dan membuktikan/membantah hipotesis teknis dengan referensi resmi Android Framework.

---

## 1. Pemetaan Alur Kode Asli Island

### 1.1 Pembuatan Profil & Provisioning
* **Island Asli:**
  * Menggunakan intent `DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE` yang diarahkan ke sistem.
  * Pasca-pembuatan profil, sistem memicu `IslandDeviceAdminReceiver.onProfileProvisioningComplete()`.
  * Receiver ini menjalankan `IslandProvisioning` (turunan `IntentService`), yang melakukan:
    1. Pengaturan afiliasi (`setAffiliationIds`).
    2. Registrasi *Cross-Profile Intent Filters* via `CrossProfileIntentFiltersHelper`.
    3. Penyalinan dan filtering aplikasi sistem via `DeleteNonRequiredAppsTask`.
    4. Aktivasi launcher proxy activity (`setupLauncherActivityInIsland`).
    5. Mengaktifkan profil dengan `setProfileEnabled()`.
  * **Kelemahan & Inefisiensi:**
    * Menggunakan `DeleteNonRequiredAppsTask` yang mengandalkan refleksi internal ke `IPackageManager` (`deletePackageAsUser`), yang memicu crash atau di-block pada Android modern (Android 9+) akibat restriksi *Hidden API*.
    * Tidak mendukung *Modern Provisioning Contract* Android 12+ (`ACTION_GET_PROVISIONING_MODE` dan `ACTION_ADMIN_POLICY_COMPLIANCE`), sehingga rentan gagal pada Android 12-15.

### 1.2 Freeze / Unfreeze & Kontrol Aplikasi
* **Island Asli:**
  * Kontrol dilakukan lewat `IslandAppControl.kt` dan `IslandManager.java`.
  * **Freeze:** Memanggil `setApplicationHidden(admin, packageName, true)` dan `setPackagesSuspended()`.
  * **Unfreeze:** Memanggil `setApplicationHidden(admin, packageName, false)` dan `setPackagesSuspended()`.
  * Jika aplikasi sistem disembunyikan, Island melacak statusnya via tabel SharedPreferences (`ClonedHiddenSystemApps.kt`).
  * **Evaluasi:** Mekanisme `setApplicationHidden` adalah API publik resmi sejak API 21 (`LOLLIPOP`) dan sangat efisien. Namun, Island membungkusnya dalam abstraksi closure IPC `Shuttle` yang berat.

### 1.3 Kloning Aplikasi ke Profil
* **Island Asli (`IslandAppClones.kt`):**
  1. *Jalur Root/ADB:* Mencoba `cmd package install-existing --user <id> <pkg>`.
  2. *Jalur Affiliated User (Android 9+):* Menggunakan `DevicePolicyManager.installExistingPackage(admin, pkg)`.
  3. *Jalur Fallback Non-Affiliated:* Mengalihkan ke `ACTION_INSTALL_PACKAGE` atau intent pasar `market://details?id=<pkg>`.
  * **Evaluasi:** Jalur `installExistingPackage` adalah standar resmi terbaik untuk performa karena tidak menyalin file APK fisik di storage (menggunakan hard link inode package oleh Android Package Manager).

### 1.4 Komunikasi Lintas Profil (IPC)
* **Island Asli:**
  * Menggunakan dua mekanisme kustom yang sangat kompleks:
    1. `ServiceShuttle`: Membuka Activity tersembunyi (`ServiceShuttleActivity`) lintas profil menggunakan `IntentForwarderActivity` sistem, lalu menyisipkan `IBinder` dispatcher di dalam Intent extras untuk menembus batas isolasi UID Android.
    2. `ShuttleProvider` (`ContentProvider`): Mengirimkan serialisasi Kotlin closure (`Closure.kt`) melalui `ContentResolver.call()` dengan skema URI `content://<userId>@com.oasisfeng.island.shuttle`.
  * **Kelemahan & Inefisiensi:**
    * Serialisasi lambdas/closures dinamis memakan alokasi heap besar dan sangat lambat (refleksi & ClassLoader).
    * `PseudoContentProvider` dan `LocalContentProvider` di Deagle dibuat sebagai pengganti Service Locator / DI yang usang, dengan multiprocess provider yang memperlambat startup aplikasi.

---

## 2. Pembuktian & Bantahan Hipotesis Teknis

### A. Pembuktian: `UserManager.requestQuietModeEnabled`
* **Hipotesis:** Baru ada sejak API 28, hanya bisa dipanggil default launcher yang sedang foreground atau app dengan `MANAGE_USERS` / `MODIFY_QUIET_MODE` (bisa di-grant via ADB).
* **Hasil Verifikasi: [TERBUKTI BENAR 100%]**
  1. **Dokumentasi & Framework API:**
     * `UserManager.requestQuietModeEnabled(boolean enable, UserHandle userHandle)` diperkenalkan pada API Level 28 (Android 9.0).
     * Level proteksi: Memerlukan izin `android.permission.MODIFY_QUIET_MODE` (`signature|privileged|development`) ATAU `android.permission.MANAGE_USERS` (signature).
     * Pengecualian pemanggil: Aplikasi yang bertindak sebagai *default home launcher* yang sedang aktif di foreground dapat memanggilnya tanpa izin signature.
  2. **Bukti di Kode Island Asli (`IslandWatcher.kt:120-145`):**
     * Island terpaksa membuat trik `DummyHomeActivity` dan foreground notification untuk mencoba memenuhi syarat pemanggilan ini, dan saat gagal menangkap `SecurityException` lalu mengarahkan user ke `Settings.ACTION_SYNC_SETTINGS`.
  3. **Solusi untuk Island-2:**
     * Dukungan grant satu kali via ADB: `adb shell pm grant <pkg> android.permission.MODIFY_QUIET_MODE`.
     * Alternatif tanpa ADB: Mendeklarasikan intent filter `CATEGORY_HOME` (opsional) atau fallback ke dialog pengaturan akun sistem.

### B. Pembuktian: Komunikasi Lintas Profil Tanpa `ServiceShuttle` & `PseudoContent`
* **Hipotesis:** Apakah IPC bisa dilakukan tanpa trik refleksi/proxy activity `ServiceShuttle`, dan dengan API publik apa?
* **Hasil Verifikasi: [TERBUKTI BISA, DENGAN DESAIN BERSIH]**
  1. **Mayoritas Operasi TIDAK Memerlukan IPC:**
     * Di Island lama, banyak hal dioperasikan via IPC padahal Android telah menyediakan API publik di sisi Personal Profile:
       * Menampilkan daftar aplikasi profil kerja: Gunakan `LauncherApps.getActivityList(null, workProfileUser)` (Tersedia resmi sejak API 21).
       * Membuka aplikasi profil kerja: Gunakan `LauncherApps.startMainActivity(component, workProfileUser, ...)` (Tersedia resmi sejak API 21).
       * Membuka App Info profil kerja: Gunakan `LauncherApps.startAppDetailsActivity(component, workProfileUser, ...)` (Tersedia resmi sejak API 21).
  2. **Operasi yang Benar-Benar Butuh Akses Profile Owner:**
     * Hanya ada 3 operasi: `setApplicationHidden` (freeze/unfreeze), `installExistingPackage` (clone), dan debloat profil.
     * Dapat diselesaikan secara aman dan cepat menggunakan **Standard Command-Pattern `ContentProvider`** dengan URI resmi bertarget user (`content://<userId>@<authority>/cmd`) dan autentikasi signature lokal via `Binder.getCallingUid()`.
     * Menghapus seluruh kebutuhan serialisasi closure/lambda dinamis (`Closure.kt`), `ServiceShuttleActivity`, dan `PseudoContentProvider`.

### C. Pembuktian: Daftar Paket Sistem Wajib & Kondisi Pasca-Provisioning
* **Hipotesis:** Paket apa yang benar-benar wajib agar Play Store dan GMS berfungsi, dan apa yang sudah otomatis tidak ter-install?
* **Hasil Verifikasi: [TERVERIFIKASI SECARA DETAIL]**
  1. **Yang Sudah Otomatis Dibatasi Sistem Android:**
     * Pada AOSP murni, `ManagedProvisioning` secara default hanya mengaktifkan paket yang terdaftar pada overlay `vendor_required_apps_managed_profile`.
     * Namun pada ROM OEM (MIUI, OneUI, ColorOS), OEM sering mengkloning seluruh bloatware mereka (Browser, Toko Aplikasi OEM, Tema, dsb.) ke profil kerja.
  2. **Daftar Paket Minimal Wajib (Essential Core):**
     * `com.android.vending` (Google Play Store)
     * `com.google.android.gms` (Google Play Services)
     * `com.google.android.gsf` (Google Services Framework)
     * `android` (Core Android OS framework)
     * `com.android.settings` (Manajemen akun, perizinan, sertifikat)
     * `com.android.keychain` (Wajib untuk TLS/Keystore; MIUI akan crash tanpa ini)
     * `com.android.providers.downloads` & `com.android.providers.downloads.ui` (DownloadManager untuk unduh file/APK Play Store)
     * `com.android.documentsui` (Document Provider / SAF UI)
     * Package Installer aktif (misal `com.google.android.packageinstaller` atau `com.android.packageinstaller`)
     * Permission Controller aktif (misal `com.google.android.permissioncontroller`)
     * Active WebView Provider (misal `com.google.android.webview` atau `com.android.webview`)
  3. **Yang Wajib Dihapus / Disembunyikan (Debloat Target):**
     * Seluruh aplikasi launcher pihak ketiga dan bloatware OEM (Gallery, Calculator, Weather, OEM App Store, Notes, Music).
     * Google bloatware: YouTube, Chrome (jika bukan webview utama), Maps, Gmail, Drive, Photos, Duo/Meet, Search Box (`com.google.android.googlequicksearchbox`).
     * Metode eksekusi: `dpm.setApplicationHidden(admin, pkg, true)`.

---

## 3. Rangkuman Keputusan Arsitektur untuk Island-2

1. **Folder Proyek:** `/root/island-2`
2. **Package Name:** `io.github.cloner`
3. **Nama Aplikasi / APK:** **Cloner** (Sub-judul: *Clean Sandbox & App Cloner*)
4. **Bahasa & Paradigma:** 100% Kotlin Modern, Coroutines, StateFlow, Manual Dependency Injection (tanpa Hilt/Koin), Jetpack Compose murni (tanpa XML UI layout).
5. **Build Optimization:** R8 Full Mode, Resource Shrinking aktif, zero third-party reflection libraries.
