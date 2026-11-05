# Cloner (Island-2)

**Cloner** adalah implementasi ulang yang sangat efisien dan ringan dari konsep Android Sandbox & App Cloner (seperti Island / Shelter) yang memanfaatkan kapabilitas bawaan Android Enterprise (Managed Work Profile).

## Keunggulan Utama
- **100% Native & Ringan:** Berjalan langsung di atas kernel Linux Android tanpa overhead virtual machine atau emulasi.
- **Zero Dangerous Reflection:** Seluruh interaksi menggunakan API publik resmi Android Enterprise (bebas crash akibat pembatasan hidden API).
- **Auto-Debloat Dinamis:** Hanya mengizinkan Google Play Store, Google Play Services, dan fondasi esensial sistem. Semua bloatware OEM dibekukan secara otomatis.
- **Quiet Mode (0% Background Overhead):** Seluruh proses dalam profil kerja dapat ditidurkan ketika tidak digunakan, menghemat 100% RAM & baterai di latar belakang.
- **Jetpack Compose UI:** Tampilan modern, responsif, dan hemat memori dengan R8 Full Mode aktif.

## Arsitektur
Lihat dokumentasi lengkap di [FINDINGS.md](FINDINGS.md).

## Lisensi
Apache License 2.0
