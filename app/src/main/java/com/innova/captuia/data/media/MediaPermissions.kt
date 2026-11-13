package com.innova.captuia.data.media

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Nivel de acceso que el usuario le dio a CaptuIA sobre sus imágenes. */
enum class MediaAccess {
    /** Acceso a todas las imágenes: permite detectar capturas nuevas automáticamente. */
    FULL,

    /** Android 14+: el usuario eligió "Seleccionar fotos"; solo se ven las elegidas. */
    PARTIAL,

    /** Sin permiso. */
    DENIED,
}

object MediaPermissions {

    private const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    private const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    private const val READ_MEDIA_VISUAL_USER_SELECTED =
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

    private const val ANDROID_13 = 33
    private const val ANDROID_14 = 34

    /** Permisos que hay que solicitar según la versión de Android del dispositivo. */
    fun toRequest(): Array<String> = when {
        Build.VERSION.SDK_INT >= ANDROID_14 ->
            arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= ANDROID_13 -> arrayOf(READ_MEDIA_IMAGES)
        else -> arrayOf(READ_EXTERNAL_STORAGE)
    }

    /** Estado real del permiso en este momento. */
    fun current(context: Context): MediaAccess {
        val sdk = Build.VERSION.SDK_INT
        return when {
            sdk >= ANDROID_13 && isGranted(context, READ_MEDIA_IMAGES) -> MediaAccess.FULL
            sdk >= ANDROID_14 && isGranted(context, READ_MEDIA_VISUAL_USER_SELECTED) ->
                MediaAccess.PARTIAL
            sdk < ANDROID_13 && isGranted(context, READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
            else -> MediaAccess.DENIED
        }
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
