package net.thunderbird.feature.changelog.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeLogManager(
    private val changelogVersionHistory: ChangelogVersionHistory,
    private val appCoroutineScope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableChangelogVersionHistoryFlow = MutableSharedFlow<ChangelogVersionHistory>(replay = 1)

    val changelog: ChangelogVersionHistory by lazy {
        mutableChangelogVersionHistoryFlow.tryEmit(changelogVersionHistory)
        changelogVersionHistory
    }

    val changelogFlow: Flow<ChangelogVersionHistory> by lazy {
        mutableChangelogVersionHistoryFlow.onSubscription {
            withContext(backgroundDispatcher) {
                // Make sure the changeLog property is initialized now if it hasn't happened before
                changelog
            }
        }
    }

    fun writeCurrentVersion() {
        appCoroutineScope.launch(backgroundDispatcher) {
            changelog.writeCurrentVersion()
            mutableChangelogVersionHistoryFlow.emit(changelog)
        }
    }
}
