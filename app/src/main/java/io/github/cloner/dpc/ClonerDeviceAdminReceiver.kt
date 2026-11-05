package io.github.cloner.dpc

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.cloner.ClonerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver administratif Device Policy Controller (Profile Owner).
 * Menerima callback sistem saat pembuatan dan inisialisasi profil kerja selesai.
 */
class ClonerDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "onProfileProvisioningComplete diterima. Memulai post-provisioning.")

        val app = context.applicationContext as ClonerApp
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.provisioner.performPostProvisioning()
            } catch (e: Exception) {
                Log.e(TAG, "Kesalahan saat post-provisioning", e)
            }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin aktif.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device Admin dinonaktifkan.")
    }

    companion object {
        private const val TAG = "Cloner.AdminReceiver"
    }
}
