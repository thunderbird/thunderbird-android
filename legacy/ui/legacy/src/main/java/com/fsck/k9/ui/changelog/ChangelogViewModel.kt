package com.fsck.k9.ui.changelog

import androidx.lifecycle.viewModelScope
import com.fsck.k9.ui.changelog.ChangelogContract.Effect
import com.fsck.k9.ui.changelog.ChangelogContract.Event
import com.fsck.k9.ui.changelog.ChangelogContract.State
import de.cketti.changelog.ReleaseItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

class ChangelogViewModel(
    private val generalSettingsManager: GeneralSettingsManager,
    private val changeLogManager: ChangeLogManager,
    private val mode: ChangeLogMode,
) : BaseViewModel<State, Event, Effect>(
    initialState = State(
        releaseItems = persistentListOf(),
        showRecentChanges = false,
    ),
) {
    init {
        viewModelScope.launch {
            loadState()
        }
    }
    private suspend fun loadState() {
        combine(
            generalSettingsManager.getConfigFlow(),
            changeLogManager.changeLogFlow,
        ) { settings, changeLog ->
            Pair(settings, changeLog)
        }.collect { (settings, changeLog) ->
            updateState { state ->
                state.copy(
                    showRecentChanges = settings.display.miscSettings.showRecentChanges,
                    releaseItems = if (mode == ChangeLogMode.CHANGE_LOG) {
                        changeLog.changeLog
                    } else {
                        changeLog.recentChanges
                    }.map { it.toReleaseUiModelList(it.versionCode == changeLog.currentVersionCode) }.toImmutableList(),
                )
            }
        }
    }

    fun setShowRecentChanges(showRecentChanges: Boolean) {
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    miscSettings = settings.display.miscSettings.copy(
                        showRecentChanges = showRecentChanges,
                    ),
                ),
            )
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnShowRecentChangesCheck -> {
                setShowRecentChanges(!event.isChecked)
            }
        }
    }

    override fun onCleared() {
        changeLogManager.writeCurrentVersion()
    }
}

enum class ChangeLogMode {
    CHANGE_LOG,
    RECENT_CHANGES,
}

enum class ChangeType {
    NEW,
    FIXED,
    CHANGED,
    ;

    override fun toString(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun asPrefix() = "$this:"
}

data class ReleaseUiModel(
    val version: String,
    val date: String?,
    val isLatest: Boolean,
    val changes: Map<String, List<String>>,
)

fun getType(change: String): ChangeType = when {
    change.startsWith(ChangeType.NEW.asPrefix()) -> ChangeType.NEW
    change.startsWith(ChangeType.FIXED.asPrefix()) -> ChangeType.FIXED
    else -> ChangeType.CHANGED
}

fun ReleaseItem.toReleaseUiModelList(isLatest: Boolean): ReleaseUiModel {
    return ReleaseUiModel(
        version = this.versionName,
        date = this.date,
        isLatest = isLatest,
        changes = this.changes.groupBy { getType(it).name }
            .mapValues { (_, list) ->
                list.map { change ->
                    val type = getType(change)
                    change.removePrefix(type.asPrefix()).trim()
                }
            },
    )
}
