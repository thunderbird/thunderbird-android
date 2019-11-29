package com.fsck.k9.ui.permissions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.K9Activity.Permission
import com.fsck.k9.ui.R

/**
 * A dialog displaying a message to explain why the app requests a certain permission.
 *
 * Closing the dialog triggers a permission request. For this to work the Activity needs to be a subclass of
 * [K9Activity].
 */
class PermissionRationaleDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments ?: error("Arguments missing")
        val permissionName = args.getString(ARG_PERMISSION) ?: error("Missing argument '$ARG_PERMISSION'")

        val permission = Permission.valueOf(permissionName)

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(permission.rationaleTitle)
            setMessage(permission.rationaleMessage)
            setPositiveButton(R.string.okay_action) { _, _ ->
                val activity = requireActivity() as? K9Activity
                        ?: throw AssertionError("PermissionRationaleDialogFragment can only be used with K9Activity")

                activity.requestPermission(permission)
            }
        }.create()
    }

    companion object {
        private const val ARG_PERMISSION = "permission"

        @JvmStatic
        fun newInstance(permission: Permission): PermissionRationaleDialogFragment {
            return PermissionRationaleDialogFragment().apply {
                arguments = bundleOf(ARG_PERMISSION to permission.name)
            }
        }
    }
}
