/*
 * Copyright (C) 2023 K-9 Mail contributors
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.k9mail.ui.utils.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Modal bottom sheet that displays a toolbar when it is fully expanded.
 *
 * Based on [com.google.android.material.bottomsheet.BottomSheetDialogFragment].
 */
open class ToolbarBottomSheetDialogFragment : AppCompatDialogFragment() {
    /**
     * Tracks if we are waiting for a dismissAllowingStateLoss or a regular dismiss once the BottomSheet is hidden and
     * onStateChanged() is called.
     */
    private var waitingForDismissAllowingStateLoss = false

    val toolbar: Toolbar?
        get() = dialog?.toolbar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ToolbarBottomSheetDialog(requireContext(), theme)
    }

    override fun dismiss() {
        if (!tryDismissWithAnimation(false)) {
            super.dismiss()
        }
    }

    override fun dismissAllowingStateLoss() {
        if (!tryDismissWithAnimation(true)) {
            super.dismissAllowingStateLoss()
        }
    }

    override fun getDialog(): ToolbarBottomSheetDialog? {
        return super.getDialog() as ToolbarBottomSheetDialog?
    }

    /**
     * Tries to dismiss the dialog fragment with the bottom sheet animation. Returns true if possible, false otherwise.
     */
    private fun tryDismissWithAnimation(allowingStateLoss: Boolean): Boolean {
        val dialog = dialog
        if (dialog != null) {
            val behavior = dialog.behavior
            if (behavior.isHideable && dialog.isDismissWithAnimation) {
                dismissWithAnimation(behavior, allowingStateLoss)
                return true
            }
        }

        return false
    }

    private fun dismissWithAnimation(behavior: BottomSheetBehavior<*>, allowingStateLoss: Boolean) {
        waitingForDismissAllowingStateLoss = allowingStateLoss

        if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            dismissAfterAnimation()
        } else {
            dialog?.removeDefaultCallback()
            behavior.addBottomSheetCallback(BottomSheetDismissCallback())
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    private fun dismissAfterAnimation() {
        if (waitingForDismissAllowingStateLoss) {
            super.dismissAllowingStateLoss()
        } else {
            super.dismiss()
        }
    }

    private inner class BottomSheetDismissCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismissAfterAnimation()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }
}
