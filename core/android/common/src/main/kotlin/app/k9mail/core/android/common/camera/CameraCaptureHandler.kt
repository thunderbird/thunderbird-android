package app.k9mail.core.android.common.camera

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import app.k9mail.core.android.common.camera.io.CaptureImageFileWriter

class CameraCaptureHandler(
    private val captureImageFileWriter: CaptureImageFileWriter,
) {

    private var capturedImageUri: Uri? = null

    companion object {
        const val REQUEST_IMAGE_CAPTURE: Int = 6
        const val CAMERA_PERMISSION_REQUEST_CODE: Int = 100

        // Kept public so callers can namespace the key inside their own
        // saved-state bundles if they prefer; the save/restore helpers
        // below use it by default.
        const val STATE_KEY_CAPTURED_IMAGE_URI: String =
            "app.k9mail.core.android.common.camera.CAPTURED_IMAGE_URI"
    }

    /**
     * Returns the URI the camera was told to write to for the last
     * [openCamera] call, or `null` if the handler has no captured URI
     * on record. Callers must handle the null case (see issue #11296:
     * when the app process is killed while the camera is in the
     * foreground, the URI is not restored and the activity result
     * arrives with data=null).
     */
    fun getCapturedImageUri(): Uri? = capturedImageUri

    /**
     * Persist the captured-image URI into [outState] so it survives
     * process death alongside the hosting Activity's state. Wire this
     * up from the Activity's `onSaveInstanceState`.
     */
    fun saveInstanceState(outState: Bundle) {
        capturedImageUri?.let { outState.putParcelable(STATE_KEY_CAPTURED_IMAGE_URI, it) }
    }

    /**
     * Restore the captured-image URI from the given saved-state bundle
     * (or clear it when [savedInstanceState] is null). Wire this up
     * from the Activity's `onCreate(savedInstanceState)`.
     */
    fun restoreInstanceState(savedInstanceState: Bundle?) {
        capturedImageUri = savedInstanceState?.let {
            @Suppress("DEPRECATION")
            it.getParcelable(STATE_KEY_CAPTURED_IMAGE_URI) as? Uri
        }
    }

    fun canLaunchCamera(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    fun openCamera(activity: Activity) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri = captureImageFileWriter.getFileUri()
        capturedImageUri = uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
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
