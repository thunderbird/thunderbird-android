package com.fsck.k9.fragment

import android.widget.AbsListView

interface HasScrollingListView {

    fun setOnScrollListenerBuildingAction(action: () -> AbsListView.OnScrollListener)
}
