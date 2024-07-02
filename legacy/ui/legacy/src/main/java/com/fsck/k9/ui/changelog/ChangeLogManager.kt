package com.fsck.k9.ui.changelog

import android.content.Context
import de.cketti.changelog.ChangeLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages a [ChangeLog] instance and notifies when its state changes.
 */
class ChangeLogManager(
    private val context: Context,
    private val appCoroutineScope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableChangeLogFlow = MutableSharedFlow<ChangeLog>(replay = 1)

    val changeLog: ChangeLog by lazy {
        ChangeLog.newInstance(context).also { changeLog ->
            mutableChangeLogFlow.tryEmit(changeLog)
        }
    }

    val changeLogFlow: Flow<ChangeLog> by lazy {
        mutableChangeLogFlow.onSubscription {
            withContext(backgroundDispatcher) {
                // Make sure the changeLog property is initialized now if it hasn't happened before
                changeLog
            }
        }
    }

    fun writeCurrentVersion() {
        appCoroutineScope.launch(backgroundDispatcher) {
            changeLog.writeCurrentVersion()

            mutableChangeLogFlow.emit(changeLog)
        }
    }
}
