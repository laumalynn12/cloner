package io.github.cloner.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Representasi data aplikasi yang terpasang di salah satu profil pengguna.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isFrozen: Boolean,
    val userHandle: UserHandle,
    val isPlayStore: Boolean = packageName == "com.android.vending",
    val isGms: Boolean = packageName == "com.google.android.gms"
)
