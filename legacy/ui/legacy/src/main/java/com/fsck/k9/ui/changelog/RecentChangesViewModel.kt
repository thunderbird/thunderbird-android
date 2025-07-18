package com.fsck.k9.ui.changelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.thunderbird.core.preference.GeneralSettingsManager

@OptIn(ExperimentalCoroutinesApi::class)
class RecentChangesViewModel(
    private val generalSettingsManager: GeneralSettingsManager,
    private val changeLogManager: ChangeLogManager,
) : ViewModel() {
    val shouldShowRecentChangesHint = changeLogManager.changeLogFlow.flatMapLatest { changeLog ->
        if (changeLog.isFirstRun && !changeLog.isFirstRunEver) {
            getShowRecentChangesFlow()
        } else {
            flowOf(false)
        }
    }.asLiveData()

    private fun getShowRecentChangesFlow(): Flow<Boolean> {
        return generalSettingsManager.getConfigFlow()
            .map { generalSettings -> generalSettings.display.showRecentChanges }
            .distinctUntilChanged()
    }

    fun onRecentChangesHintDismissed() {
        changeLogManager.writeCurrentVersion()
    }
}
