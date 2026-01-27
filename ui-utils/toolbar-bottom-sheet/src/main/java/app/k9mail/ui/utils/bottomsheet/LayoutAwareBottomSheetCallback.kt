package app.k9mail.ui.utils.bottomsheet

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

/**
 * Extends [BottomSheetCallback] so we can notify listeners of layout events.
 *
 * See [LayoutAwareBottomSheetBehavior].
 */
internal abstract class LayoutAwareBottomSheetCallback : BottomSheetCallback() {
    abstract fun onBottomSheetLayout(bottomSheet: View)
}
