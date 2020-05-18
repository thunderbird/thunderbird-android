package com.fsck.k9.ui.permissions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.fsck.k9.ui.R

/**
 * A dialog displaying a message to explain why the app requests a certain permission.
 *
 * Closing the dialog triggers a permission request. For this to work the Activity needs to implement
 * [PermissionUiHelper].
 */
class PermissionRationaleDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments ?: error("Arguments missing")
        val permissionName = args.getString(ARG_PERMISSION) ?: error("Missing argument '$ARG_PERMISSION'")

        val permission = Permission.valueOf(permissionName)

        return AlertDialog.Builder(requireContext())
            .setTitle(permission.rationaleTitle)
            .setMessage(permission.rationaleMessage)
            .setPositiveButton(R.string.okay_action) { _, _ ->
                val permissionUiHelper = requireActivity() as? PermissionUiHelper
                        ?: throw AssertionError("Activities using PermissionRationaleDialogFragment need to " +
                            "implement PermissionUiHelper")

                permissionUiHelper.requestPermission(permission)
            }.create()
    }

    companion object {
        private const val ARG_PERMISSION = "permission"

        fun newInstance(permission: Permission): PermissionRationaleDialogFragment {
            return PermissionRationaleDialogFragment().apply {
                arguments = bundleOf(ARG_PERMISSION to permission.name)
            }
        }
    }
}
