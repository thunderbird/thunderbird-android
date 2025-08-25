package com.fsck.k9.ui.base.loader

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.logging.legacy.Log

const val LOADING_INDICATOR_DELAY = 500L

/**
 * Load data in an I/O thread. Updates the returned [LiveData] with the current loading state.
 *
 * If loading takes longer than [LOADING_INDICATOR_DELAY] the [LoaderState.Loading] state will be emitted so the UI can
 * display a loading indicator. We use a delay so fast loads won't flash a loading indicator.
 * If an exception is thrown during loading the [LoaderState.Error] state is emitted.
 * If the data was loaded successfully [LoaderState.Data] will be emitted containing the data.
 */
fun <T> liveDataLoader(block: CoroutineScope.() -> T): LiveData<LoaderState<T>> = liveData {
    coroutineScope {
        val job = launch {
            delay(LOADING_INDICATOR_DELAY)

            // Emit loading state if loading took longer than configured delay. If the data was loaded faster than that,
            // this coroutine will have been canceled before the next line is executed.
            emit(LoaderState.Loading)
        }

        val finalState = try {
            val data = withContext(Dispatchers.IO) {
                block()
            }

            LoaderState.Data(data)
        } catch (e: Exception) {
            Log.e(e, "Error loading data")
            LoaderState.Error
        }

        // Cancel job that emits Loading state
        job.cancelAndJoin()

        emit(finalState)
    }
}

sealed class LoaderState<out T> {
    object Loading : LoaderState<Nothing>()
    object Error : LoaderState<Nothing>()
    class Data<T>(val data: T) : LoaderState<T>()
}
