package com.fsck.k9.ui.changelog

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fsck.k9.preferences.GeneralSettingsManager
import com.fsck.k9.ui.base.loader.LoaderState
import com.fsck.k9.ui.base.loader.liveDataLoader
import de.cketti.changelog.ReleaseItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private typealias ChangeLogState = LoaderState<List<ReleaseItem>>

class ChangelogViewModel(
    private val generalSettingsManager: GeneralSettingsManager,
    private val changeLogManager: ChangeLogManager,
    private val mode: ChangeLogMode,
) : ViewModel() {
    val showRecentChangesState: LiveData<Boolean> =
        generalSettingsManager.getSettingsFlow()
            .map { it.showRecentChanges }
            .distinctUntilChanged()
            .asLiveData()

    val changelogState: LiveData<ChangeLogState> = liveDataLoader {
        val changeLog = changeLogManager.changeLog
        when (mode) {
            ChangeLogMode.CHANGE_LOG -> changeLog.changeLog
            ChangeLogMode.RECENT_CHANGES -> changeLog.recentChanges
        }
    }

    fun setShowRecentChanges(showRecentChanges: Boolean) {
        generalSettingsManager.setShowRecentChanges(showRecentChanges)
    }

    override fun onCleared() {
        changeLogManager.writeCurrentVersion()
    }
}

enum class ChangeLogMode {
    CHANGE_LOG,
    RECENT_CHANGES,
}
