package com.fsck.k9.ui.helper

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class CoroutineScopeViewModel : ViewModel(), CoroutineScope {
    private val job = Job()

    override val coroutineContext = Dispatchers.Main + job

    override fun onCleared() {
        job.cancel()
    }
}
