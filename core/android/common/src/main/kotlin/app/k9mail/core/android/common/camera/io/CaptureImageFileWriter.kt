package app.k9mail.core.android.common.camera.io

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class CaptureImageFileWriter(private val context: Context) {

    fun getFileUri(): Uri {
        val file = getCaptureImageFile()
        return FileProvider.getUriForFile(context, "${context.packageName}.activity", file)
    }

    private fun getCaptureImageFile(): File {
        val fileName = "IMG_${System.currentTimeMillis()}$FILE_EXT"
        return File(getDirectory(), fileName)
    }

    private fun getDirectory(): File {
        val directory = File(context.cacheDir, DIRECTORY_NAME)
        directory.mkdirs()

        return directory
    }

    companion object {
        private const val FILE_EXT = ".jpg"
        private const val DIRECTORY_NAME = "captureImage"
    }
}
