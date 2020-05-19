package com.fsck.k9.ui.permissions

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

interface PermissionUiHelper {
    fun hasPermission(permission: Permission): Boolean
    fun requestPermissionOrShowRationale(permission: Permission)
    fun requestPermission(permission: Permission)
}

class K9PermissionUiHelper(private val activity: AppCompatActivity) : PermissionUiHelper {
    override fun hasPermission(permission: Permission): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission.permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermissionOrShowRationale(permission: Permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.permission)) {
            val dialogFragment = PermissionRationaleDialogFragment.newInstance(permission)
            dialogFragment.show(activity.supportFragmentManager, FRAGMENT_TAG_RATIONALE)
        } else {
            requestPermission(permission)
        }
    }

    override fun requestPermission(permission: Permission) {
        Timber.i("Requesting permission: " + permission.permission)
        ActivityCompat.requestPermissions(activity, arrayOf(permission.permission), permission.requestCode)
    }

    companion object {
        private const val FRAGMENT_TAG_RATIONALE = "rationale"
    }
}
