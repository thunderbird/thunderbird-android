package com.fsck.k9.ui.drawer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R

/**
 * Hack to make MaterialDrawer's sticky footer work with our swipe-to-refresh hack :(
 *
 * MaterialDrawer changes the LayoutParams on the RecyclerView so it ends above the sticky header. But because we
 * changed the view hierarchy to support swipe-to-refresh this no longer works. So we intercept the LayoutParams change
 * and forward it to the SwipeRefreshLayout instead.
 */
class MaterialDrawerHackRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        if (params is RelativeLayout.LayoutParams) {
            val layoutAboveValue = params.rules[RelativeLayout.ABOVE]
            if (layoutAboveValue != 0) {
                val containerLayout = rootView.findViewById<View>(R.id.material_drawer_swipe_refresh)
                val containerParams = containerLayout.layoutParams as RelativeLayout.LayoutParams
                containerParams.addRule(RelativeLayout.ABOVE, layoutAboveValue)
                containerLayout.layoutParams = containerParams
                return
            }
        }

        super.setLayoutParams(params)
    }
}
