package io.github.cloner.dpc

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Activity yang wajib disediakan untuk perangkat Android 12+ (API 31+)
 * saat sistem menanyakan mode provisioning yang diinginkan.
 */
class ProvisioningModeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Menerima permintaan ACTION_GET_PROVISIONING_MODE dari sistem Android 12+.")

        val resultIntent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            resultIntent.putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
            )
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        private const val TAG = "Cloner.ProvModeAct"
    }
}

/**
 * Activity kepatuhan kebijakan Android 12+ (API 31+) sebelum setup pengguna ditutup.
 */
class PolicyComplianceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Menerima ACTION_ADMIN_POLICY_COMPLIANCE.")
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val TAG = "Cloner.PolicyCompliance"
    }
}
