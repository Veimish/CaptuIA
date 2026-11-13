package com.innova.captuia.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.innova.captuia.domain.model.DeviceScreenshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee las capturas de pantalla que existen en la galería del dispositivo
 * a través de MediaStore. No copia ni modifica ningún archivo.
 *
 * Una imagen se considera captura si está en una carpeta de capturas
 * (p. ej. Pictures/Screenshots o DCIM/Screenshots) o si su nombre empieza
 * por "Screenshot" o "Captura", que es como las nombran los fabricantes.
 */
class ScreenshotSource(private val resolver: ContentResolver) {

    /** Todas las capturas del dispositivo, de la más reciente a la más antigua. */
    suspend fun loadAll(): List<DeviceScreenshot> = withContext(Dispatchers.IO) {
        val collection: Uri = imagesCollection()
        val pathColumn = pathColumn()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
        )
        val nameColumn = MediaStore.Images.Media.DISPLAY_NAME
        val selection =
            "($pathColumn LIKE ? OR $pathColumn LIKE ? OR $nameColumn LIKE ? OR $nameColumn LIKE ?)"
        val selectionArgs = arrayOf("%screenshot%", "%captura%", "screenshot%", "captura%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val result = mutableListOf<DeviceScreenshot>()
        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                result += DeviceScreenshot(
                    mediaId = id,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    // MediaStore guarda DATE_ADDED en segundos.
                    dateAddedMillis = cursor.getLong(dateIndex) * 1000L,
                    width = cursor.getInt(widthIndex),
                    height = cursor.getInt(heightIndex),
                    sizeBytes = cursor.getLong(sizeIndex),
                )
            }
        }
        result
    }

    private fun imagesCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    /** Android 10+ expone la carpeta relativa; versiones anteriores, la ruta completa. */
    @Suppress("DEPRECATION")
    private fun pathColumn(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }
}
