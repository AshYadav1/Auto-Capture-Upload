
package com.app.autocaptureandupload.camerautils.repository


import android.content.Context
import android.content.Intent
import android.hardware.Camera.ACTION_NEW_PICTURE
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.app.autocaptureandupload.R
import java.io.File
import java.util.*

/**
 * Manages photo capture filename and location generation. Once a photo is captured and saved to
 * disk, the repository will also notify that the image has been created such that other
 * applications can view it.
 */
class ImageCaptureRepository internal constructor(private val rootDirectory: File) {
    companion object {
        const val TAG = "ImageCaptureRepository"

        private const val PHOTO_EXTENSION = ".jpg"

        fun create(context: Context): ImageCaptureRepository {
            // Use external media if it is available and this app's file directory otherwise
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            val file = if (mediaDir?.exists() == true) mediaDir else appContext.filesDir
            return ImageCaptureRepository(file)
        }
    }

    fun notifyImageCreated(context: Context, savedUri: Uri) {
        val file = savedUri.toFile()
        val fileProviderUri =
            FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        context.sendBroadcast(
            Intent(ACTION_NEW_PICTURE, fileProviderUri)
        )

        // Notify other apps so they can access the captured image
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension)

        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
        ) { _, _ -> }
    }

    fun createImageOutputFile(): File = File(rootDirectory, generateFilename(PHOTO_EXTENSION))

    private fun generateFilename(extension: String): String =
        UUID.randomUUID().toString() + extension
}
