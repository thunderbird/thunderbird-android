package com.fsck.k9.fragment

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ProgressDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val title = args.getString(ARG_TITLE)
        val message = args.getString(ARG_MESSAGE)

        return ProgressDialog(requireActivity()).apply {
            isIndeterminate = true
            setTitle(title)
            setMessage(message)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        (activity as? CancelListener)?.onProgressCancel(this)
        super.onCancel(dialog)
    }

    interface CancelListener {
        fun onProgressCancel(fragment: ProgressDialogFragment)
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        fun newInstance(title: String?, message: String?): ProgressDialogFragment {
            return ProgressDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                }
            }
        }
    }
}
