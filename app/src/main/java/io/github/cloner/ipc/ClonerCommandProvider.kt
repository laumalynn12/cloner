package io.github.cloner.ipc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.util.Log
import io.github.cloner.ClonerApp

/**
 * Endpoint IPC di dalam Profil Kerja.
 * Menerima perintah dari profil utama dan menjalankannya dengan hak Profile Owner.
 * Memvalidasi AppId pemanggil agar pihak ketiga tidak dapat mengeksploitasi endpoint ini.
 */
class ClonerCommandProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        validateCallerSecurity()

        val app = context?.applicationContext as? ClonerApp
            ?: throw IllegalStateException("Aplikasi belum siap")

        val resultBundle = Bundle()
        val targetPackage = arg ?: extras?.getString(ClonerIpcContract.EXTRA_PACKAGE_NAME)

        Log.d(TAG, "Menerima IPC call: method=$method, targetPackage=$targetPackage")

        when (method) {
            ClonerIpcContract.METHOD_PING -> {
                resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, true)
                resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "PONG")
            }

            ClonerIpcContract.METHOD_FREEZE -> {
                if (targetPackage == null) {
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, false)
                    resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "Nama paket kosong")
                } else {
                    val success = app.dpcManager.setApplicationHidden(targetPackage, true)
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, success)
                }
            }

            ClonerIpcContract.METHOD_UNFREEZE -> {
                if (targetPackage == null) {
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, false)
                    resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "Nama paket kosong")
                } else {
                    val success = app.dpcManager.setApplicationHidden(targetPackage, false)
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, success)
                }
            }

            ClonerIpcContract.METHOD_IS_FROZEN -> {
                val isFrozen = targetPackage?.let { app.dpcManager.isApplicationHidden(it) } ?: false
                resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, isFrozen)
            }

            ClonerIpcContract.METHOD_INSTALL_EXISTING -> {
                if (targetPackage == null) {
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, false)
                    resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "Nama paket kosong")
                } else {
                    val success = app.dpcManager.installExistingPackage(targetPackage)
                    resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, success)
                }
            }

            ClonerIpcContract.METHOD_TRIGGER_DEBLOAT -> {
                val debloated = app.provisioner.executeDynamicDebloat()
                resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, true)
                resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "$debloated paket dibersihkan")
            }

            else -> {
                resultBundle.putBoolean(ClonerIpcContract.EXTRA_RESULT_SUCCESS, false)
                resultBundle.putString(ClonerIpcContract.EXTRA_RESULT_MESSAGE, "Metode tidak dikenal: $method")
            }
        }

        return resultBundle
    }

    private fun validateCallerSecurity() {
        val callingUid = Binder.getCallingUid()
        val myUid = Process.myUid()

        // Verifikasi AppId: Pemanggil di user 0 dan penerima di user 10 memiliki AppId yang sama (karena satu paket APK)
        val callingAppId = UserHandle.getUserHandleForUid(callingUid).hashCode().let { callingUid % 100000 }
        val myAppId = myUid % 100000

        if (callingAppId != myAppId && callingUid != Process.SYSTEM_UID) {
            Log.e(TAG, "Ditolak: Calling UID $callingUid tidak memiliki AppId yang cocok dengan $myAppId")
            throw SecurityException("Akses ditolak: Hanya Cloner dari profil pemilik yang berwenang.")
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val TAG = "Cloner.CommandProvider"
    }
}
