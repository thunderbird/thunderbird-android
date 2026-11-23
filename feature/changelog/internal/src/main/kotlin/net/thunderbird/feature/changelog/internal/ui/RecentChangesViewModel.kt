package net.thunderbird.feature.changelog.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.changelog.internal.ChangeLogManager
import net.thunderbird.feature.changelog.internal.ui.RecentChangesContract.Effect
import net.thunderbird.feature.changelog.internal.ui.RecentChangesContract.Event
import net.thunderbird.feature.changelog.internal.ui.RecentChangesContract.State

@OptIn(ExperimentalCoroutinesApi::class)
class RecentChangesViewModel(
    private val generalSettingsManager: GeneralSettingsManager,
    private val changeLogManager: ChangeLogManager,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    RecentChangesContract.ViewModel {

    init {
        changeLogManager.changelogFlow
            .flatMapLatest { changeLog ->
                if (changeLog.isFirstRun() && !changeLog.isFirstRunEver()) {
                    getShowRecentChangesFlow()
                } else {
                    flowOf(false)
                }
            }
            .onEach { showRecentChangesHint ->
                updateState { it.copy(showRecentChangesHint = showRecentChangesHint) }
            }
            .launchIn(viewModelScope)
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnRecentChangesHintDismissed -> changeLogManager.writeCurrentVersion()
        }
    }

    private fun getShowRecentChangesFlow(): Flow<Boolean> {
        return generalSettingsManager.getConfigFlow()
            .map { generalSettings -> generalSettings.display.miscSettings.showRecentChanges }
            .distinctUntilChanged()
    }
}
