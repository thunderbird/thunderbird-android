package com.fsck.k9.ui.changelog

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.fsck.k9.ui.changelog.ChangeLogState.Data
import com.fsck.k9.ui.changelog.ChangeLogState.Error
import com.fsck.k9.ui.changelog.ChangeLogState.Loading
import de.cketti.changelog.ChangeLog
import de.cketti.changelog.ReleaseItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val PROGRESS_BAR_DELAY = 500L

class ChangelogViewModel(private val context: Context) : ViewModel() {
    val changelogState: LiveData<ChangeLogState> = liveData {
        coroutineScope {
            loadChangelog(coroutineScope = this)
        }
    }

    private suspend fun LiveDataScope<ChangeLogState>.loadChangelog(coroutineScope: CoroutineScope) {
        val job = coroutineScope.launch {
            delay(PROGRESS_BAR_DELAY)

            // Show progress bar if loading took longer than configured delay. If changelog data was loaded faster
            // than that, this coroutine will have been canceled before the next line is executed.
            emit(Loading)
        }

        val finalState = try {
            val data = withContext(Dispatchers.IO) {
                val changeLog = ChangeLog.newInstance(context)
                changeLog.changeLog
            }

            Data(data)
        } catch (e: Exception) {
            Timber.e(e, "Error loading changelog")
            Error
        }

        // Cancel job that emits Loading state
        job.cancelAndJoin()

        emit(finalState)
    }
}

sealed class ChangeLogState {
    object Loading : ChangeLogState()
    object Error : ChangeLogState()
    class Data(val changeLog: List<ReleaseItem>) : ChangeLogState()
}
