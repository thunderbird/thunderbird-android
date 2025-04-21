package app.k9mail.core.android.common.camera

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import app.k9mail.core.android.common.camera.io.CaptureImageFileWriter

class CameraCaptureHandler(
    private val captureImageFileWriter: CaptureImageFileWriter,
) {

    private lateinit var capturedImageUri: Uri

    companion object {
        const val REQUEST_IMAGE_CAPTURE: Int = 6
        const val CAMERA_PERMISSION_REQUEST_CODE: Int = 100
    }

    fun getCapturedImageUri(): Uri {
        if (::capturedImageUri.isInitialized) {
            return capturedImageUri
        } else {
            throw UninitializedPropertyAccessException("Image Uri not initialized")
        }
    }

    fun canLaunchCamera(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    fun openCamera(activity: Activity) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturedImageUri = captureImageFileWriter.getFileUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(activity, intent, REQUEST_IMAGE_CAPTURE, null)
    }

    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE,
        )
    }

    fun hasCameraPermission(context: Context): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(context, permission.CAMERA)
        return hasPermission == PackageManager.PERMISSION_GRANTED
    }
}
