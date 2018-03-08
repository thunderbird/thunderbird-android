package com.fsck.k9.ui

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: (T) -> Unit) {
    this.observe(owner, Observer<T> { observer(it!!) })
}
