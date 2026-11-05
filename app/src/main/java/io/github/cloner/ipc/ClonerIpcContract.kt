package io.github.cloner.ipc

import android.net.Uri

/**
 * Kontrak IPC inter-user berbasis Command-Pattern ContentProvider.
 * Menggantikan ServiceShuttle dan serialisasi closure Island lama.
 */
object ClonerIpcContract {
    const val AUTHORITY = "io.github.cloner.ipc"

    // Perintah DPC Lintas Profil
    const val METHOD_FREEZE = "freeze"
    const val METHOD_UNFREEZE = "unfreeze"
    const val METHOD_INSTALL_EXISTING = "install_existing"
    const val METHOD_IS_FROZEN = "is_frozen"
    const val METHOD_TRIGGER_DEBLOAT = "trigger_debloat"
    const val METHOD_PING = "ping"

    // Argumen dan Return Keys
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_RESULT_SUCCESS = "extra_result_success"
    const val EXTRA_RESULT_MESSAGE = "extra_result_message"

    fun buildUserTargetUri(userId: Int): Uri {
        return Uri.parse("content://$userId@$AUTHORITY")
    }
}
