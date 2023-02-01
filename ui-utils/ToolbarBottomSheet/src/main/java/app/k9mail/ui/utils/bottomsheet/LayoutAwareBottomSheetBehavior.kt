package app.k9mail.ui.utils.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

/**
 * Work around the fact that [BottomSheetCallback.onLayout] is not public.
 */
class LayoutAwareBottomSheetBehavior<V : View>(context: Context, attrs: AttributeSet?) :
    BottomSheetBehavior<V>(context, attrs) {

    private val callbacks = mutableSetOf<LayoutAwareBottomSheetCallback>()

    internal fun addBottomSheetCallback(callback: LayoutAwareBottomSheetCallback) {
        callbacks.add(callback)
        super.addBottomSheetCallback(callback)
    }

    internal fun removeBottomSheetCallback(callback: LayoutAwareBottomSheetCallback) {
        super.removeBottomSheetCallback(callback)
        callbacks.remove(callback)
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        val layoutResult = super.onLayoutChild(parent, child, layoutDirection)

        for (callback in callbacks) {
            callback.onBottomSheetLayout(child)
        }

        return layoutResult
    }

    companion object {
        fun <V : View> from(view: V): LayoutAwareBottomSheetBehavior<V> {
            val params = view.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "The view is not a child of CoordinatorLayout" }

            val behavior = params.behavior
            require(behavior is LayoutAwareBottomSheetBehavior<*>) {
                "The view is not associated with ToolbarBottomSheetBehavior"
            }

            @Suppress("UNCHECKED_CAST")
            return behavior as LayoutAwareBottomSheetBehavior<V>
        }
    }
}
