package com.fsck.k9.ui.fab

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * Shrink the supplied [ExtendedFloatingActionButton] when the RecyclerView this listener is attached to is scrolling
 * down, and expand the FAB when scrolling up.
 */
class ShrinkFabOnScrollListener(private val floatingActionButton: ExtendedFloatingActionButton) : OnScrollListener() {
    private var isScrolledUp = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > 0) {
            if (recyclerView.canScrollVertically(1)) {
                shrink()
            } else {
                extend()
            }
        } else if (dy < 0) {
            extend()
        }
    }

    private fun extend() {
        if (!isScrolledUp) {
            isScrolledUp = true
            floatingActionButton.extend()
        }
    }

    private fun shrink() {
        if (isScrolledUp) {
            isScrolledUp = false
            floatingActionButton.shrink()
        }
    }
}
