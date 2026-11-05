package io.github.cloner.model

import android.os.UserHandle

/**
 * Status profil kerja terisolasi (Sandbox).
 */
sealed interface ProfileState {
    data object NotCreated : ProfileState
    data class Active(val userHandle: UserHandle) : ProfileState
    data class Quiet(val userHandle: UserHandle) : ProfileState
    data class Disabled(val userHandle: UserHandle) : ProfileState
}
