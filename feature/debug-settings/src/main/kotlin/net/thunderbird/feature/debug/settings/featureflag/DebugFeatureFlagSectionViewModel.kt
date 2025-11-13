package net.thunderbird.feature.debug.settings.featureflag

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.MutableFeatureFlagFactory
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Effect
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Event
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.State
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.ViewModel

class DebugFeatureFlagSectionViewModel(
    private val featureFlagFactory: MutableFeatureFlagFactory,
) : BaseViewModel<State, Event, Effect>(
    initialState = State(
        defaults = featureFlagFactory.defaults.associateBy { it.key }.toPersistentMap(),
        overrides = featureFlagFactory.overrides.toPersistentMap(),
    ),
),
    ViewModel {
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
            featureFlagFactory.restoreDefaults()
        } else {
            state.value.pendingOverrides.forEach { (key, enabled) ->
                featureFlagFactory.override(key = key, enabled = enabled)
            }
        }
        emitEffect(Effect.RestartMainActivity)
    }

    private fun toggleFeatureFlag(flag: FeatureFlag) {
        updateState { state ->
            val value = state.pendingOverrides[flag.key]
                ?: state.overrides[flag.key]
                ?: state.defaults[flag.key]?.enabled
                ?: false
            val overrides = state.pendingOverrides + (flag.key to !value)
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
