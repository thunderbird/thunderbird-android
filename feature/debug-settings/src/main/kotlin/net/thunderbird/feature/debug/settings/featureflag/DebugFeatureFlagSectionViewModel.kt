package net.thunderbird.feature.debug.settings.featureflag

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagOverrides
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Effect
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Event
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.State
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.ViewModel

class DebugFeatureFlagSectionViewModel(
    private val featureFlagFactory: FeatureFlagFactory,
    private val featureFlagOverrides: FeatureFlagOverrides,
) : BaseViewModel<State, Event, Effect>(
    initialState = State(
        defaults = featureFlagFactory.createFeatureCatalog().associateBy { it.key }.toPersistentMap(),
    ),
),
    ViewModel {

    init {
        featureFlagOverrides
            .overrides
            .onEach { overrides ->
                updateState { it.copy(overrides = overrides.toImmutableMap()) }
            }
            .launchIn(viewModelScope)
    }

    override fun event(event: Event) {
        when (event) {
            Event.ApplyChanges -> applyChanges()
            is Event.OnToggle -> toggleFeatureFlag(event.flag)
            Event.RestoreDefaults -> restoreDefaults()
        }
    }

    private fun applyChanges() {
        val current = state.value
        val defaults = current
            .defaults
            .mapValues { it.value.enabled }
            .filter { it.key in current.pendingOverrides }
        if (current.pendingOverrides == defaults) {
            featureFlagOverrides.clearAll()
        } else {
            state.value.pendingOverrides.forEach { (key, enabled) ->
                featureFlagOverrides[key] = enabled
            }
        }
        emitEffect(Effect.RestartMainActivity)
    }

    private fun toggleFeatureFlag(flag: FeatureFlag) {
        updateState { state ->
            val currentActiveValue = state.overrides[flag.key]
                ?: state.defaults[flag.key]?.enabled
            val overrides = if (
                flag.key in state.pendingOverrides &&
                state.pendingOverrides[flag.key]?.not() == currentActiveValue
            ) {
                state.pendingOverrides - flag.key
            } else {
                val value = state.pendingOverrides[flag.key]
                    ?: currentActiveValue
                    ?: false
                state.pendingOverrides + (flag.key to !value)
            }
            emitEffect(Effect.NotifyPendingChanges(pendingOverrides = overrides.toPersistentMap()))
            state.copy(pendingOverrides = overrides.toPersistentMap())
        }
    }

    private fun restoreDefaults() {
        updateState { state ->
            val pendingOverrides = state.pendingOverrides
            val currentOverrides = state.overrides
            if (currentOverrides.isEmpty()) {
                state.copy(pendingOverrides = persistentMapOf())
            } else {
                val defaults = state.defaults.filter { (key, _) ->
                    key in pendingOverrides || key in currentOverrides
                }
                val defaultOverrides = defaults.mapValues { it.value.enabled }.toPersistentMap()
                emitEffect(Effect.NotifyPendingChanges(pendingOverrides = defaultOverrides))
                state.copy(
                    pendingOverrides = defaultOverrides,
                )
            }
        }
    }
}
