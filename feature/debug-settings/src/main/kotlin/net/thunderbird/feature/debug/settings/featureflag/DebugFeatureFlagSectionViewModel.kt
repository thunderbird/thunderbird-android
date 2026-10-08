package net.thunderbird.feature.debug.settings.featureflag

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.provider.BundledFeatureFlagDefaults
import net.thunderbird.core.featureflag.provider.RuntimeDebugOverrideFeatureFlagProvider
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Effect
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.Event
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.State
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSectionContract.ViewModel

class DebugFeatureFlagSectionViewModel(
    bundleDefaults: BundledFeatureFlagDefaults,
    private val runtimeProvider: RuntimeDebugOverrideFeatureFlagProvider,
) : BaseViewModel<State, Event, Effect>(initialState = State()), ViewModel {

    init {
        val baseline = bundleDefaults.defaults()
        val defaults = baseline.toFeatureFlagKeyMap()
        updateState { state -> state.copy(defaults = defaults) }

        runtimeProvider
            .overrides
            .onEach { overrides ->
                updateState { state ->
                    state.copy(
                        overrides = overrides.mapKeys { (key, _) -> key.toFeatureFlagKey() }.toImmutableMap(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun event(event: Event) {
        when (event) {
            Event.ApplyChanges -> applyChanges()
            is Event.OnToggle -> toggleFeatureFlag(event.key)
            Event.RestoreDefaults -> restoreDefaults()
        }
    }

    private fun applyChanges() {
        viewModelScope.launch {
            val current = state.value
            val defaults = current
                .defaults
                .filter { it.key in current.pendingOverrides }
            if (current.pendingOverrides == defaults) {
                runtimeProvider.clearAllOverrides()
            } else {
                state.value.pendingOverrides.forEach { (key, enabled) ->
                    runtimeProvider.setOverride(key, enabled)
                }
            }
            emitEffect(Effect.RestartMainActivity)
        }
    }

    private fun toggleFeatureFlag(flagKey: FeatureFlagKey) {
        updateState { state ->
            val currentActiveValue = state.overrides[flagKey]
                ?: state.defaults[flagKey]
            val overrides = if (
                flagKey in state.pendingOverrides &&
                state.pendingOverrides[flagKey]?.not() == currentActiveValue
            ) {
                state.pendingOverrides - flagKey
            } else {
                val value = state.pendingOverrides[flagKey]
                    ?: currentActiveValue
                    ?: false
                state.pendingOverrides + (flagKey to !value)
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
                val defaultOverrides = defaults.mapValues { it.value }.toPersistentMap()
                emitEffect(Effect.NotifyPendingChanges(pendingOverrides = defaultOverrides))
                state.copy(
                    pendingOverrides = defaultOverrides,
                )
            }
        }
    }

    private fun Map<String, Boolean>.toFeatureFlagKeyMap(): ImmutableMap<FeatureFlagKey, Boolean> =
        this.entries.associate { (key, value) ->
            val flagKey: FeatureFlagKey = key.toFeatureFlagKey()
            flagKey to value
        }.toImmutableMap()

    private fun String.toFeatureFlagKey(): FeatureFlagKey = GeneratedFeatureFlagKey.valueOf(uppercase())
}
