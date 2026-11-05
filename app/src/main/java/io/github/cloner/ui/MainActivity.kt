package io.github.cloner.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.cloner.ClonerApp
import io.github.cloner.ui.screens.MainScreen
import io.github.cloner.ui.theme.ClonerTheme
import io.github.cloner.ui.viewmodel.MainViewModel

/**
 * Activity utama aplikasi Cloner berbasis Jetpack Compose murni.
 */
class MainActivity : ComponentActivity() {

    private val app by lazy { application as ClonerApp }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = app.repository,
            freezeManager = app.freezeManager,
            cloneManager = app.cloneManager,
            quietModeManager = app.quietModeManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClonerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onCreateProfileClick = {
                        app.repository.startManagedProfileProvisioning(
                            activity = this,
                            requestCode = REQUEST_PROVISION_PROFILE
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROVISION_PROFILE) {
            Toast.makeText(this, "Sedang menyiapkan profil otomatis...", Toast.LENGTH_SHORT).show()
            viewModel.refresh()
        }
    }

    companion object {
        private const val TAG = "Cloner.MainActivity"
        private const val REQUEST_PROVISION_PROFILE = 1001
    }
}
