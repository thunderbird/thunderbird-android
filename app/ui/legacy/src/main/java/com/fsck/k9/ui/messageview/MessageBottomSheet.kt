package com.fsck.k9.ui.messageview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import app.k9mail.ui.utils.bottomsheet.ToolbarBottomSheetDialogFragment
import com.fsck.k9.ui.R

class MessageBottomSheet : ToolbarBottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.message_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = checkNotNull(dialog)
        dialog.isDismissWithAnimation = true

        val toolbar = checkNotNull(toolbar)
        toolbar.apply {
            title = "Message details"
            navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)

            setNavigationOnClickListener {
                dismiss()
            }
        }
    }
}
