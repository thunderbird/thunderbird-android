package com.fsck.k9.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

fun <T> LiveData<T>.observeNotNull(owner: LifecycleOwner, observer: (T) -> Unit) {
    this.observe(owner) { observer(it!!) }
}
