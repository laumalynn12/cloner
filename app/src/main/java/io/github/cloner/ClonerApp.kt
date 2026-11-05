package io.github.cloner

import android.app.Application
import io.github.cloner.dpc.DpcManager
import io.github.cloner.ipc.ClonerIpcClient
import io.github.cloner.manager.AppCloneManager
import io.github.cloner.manager.AppFreezeManager
import io.github.cloner.manager.QuietModeManager
import io.github.cloner.provisioning.DynamicWhitelistResolver
import io.github.cloner.provisioning.ProfileProvisioner
import io.github.cloner.repository.AppRepository

/**
 * Kelas Aplikasi Utama dengan kontainer Dependency Injection manual.
 * Mengeliminasi overhead runtime refleksi Hilt / Koin untuk performa startup instan.
 */
class ClonerApp : Application() {

    val dpcManager: DpcManager by lazy { DpcManager(this) }
    val whitelistResolver: DynamicWhitelistResolver by lazy { DynamicWhitelistResolver(this) }
    val provisioner: ProfileProvisioner by lazy {
        ProfileProvisioner(this, dpcManager, whitelistResolver)
    }
    val ipcClient: ClonerIpcClient by lazy { ClonerIpcClient(this) }
    val freezeManager: AppFreezeManager by lazy {
        AppFreezeManager(this, dpcManager, ipcClient)
    }
    val cloneManager: AppCloneManager by lazy { AppCloneManager(this, ipcClient) }
    val quietModeManager: QuietModeManager by lazy { QuietModeManager(this) }
    val repository: AppRepository by lazy {
        AppRepository(this, dpcManager, freezeManager, quietModeManager)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
