package com.fsck.k9.ui.base.loader

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Use to observe the [LiveData] returned by [liveDataLoader].
 *
 * Works with separate views for the [LoaderState.Loading], [LoaderState.Error], and [LoaderState.Data] states. The
 * view associated with the current state is made visible and the others are hidden. For the [LoaderState.Data] state
 * the [displayData] function is also called.
 */
fun <T> LiveData<LoaderState<T>>.observeLoading(
    owner: LifecycleOwner,
    loadingView: View,
    errorView: View,
    dataView: View,
    displayData: (T) -> Unit,
) {
    observe(owner, LoaderStateObserver(loadingView, errorView, dataView, displayData))
}

private class LoaderStateObserver<T>(
    private val loadingView: View,
    private val errorView: View,
    private val dataView: View,
    private val displayData: (T) -> Unit,
) : Observer<LoaderState<T>> {
    private val allViews = setOf(loadingView, errorView, dataView)

    override fun onChanged(value: LoaderState<T>) {
        when (value) {
            is LoaderState.Loading -> loadingView.show()
            is LoaderState.Error -> errorView.show()
            is LoaderState.Data -> {
                dataView.show()
                displayData(value.data)
            }
        }
    }

    private fun View.show() {
        for (view in allViews) {
            view.isVisible = view === this
        }
    }
}
