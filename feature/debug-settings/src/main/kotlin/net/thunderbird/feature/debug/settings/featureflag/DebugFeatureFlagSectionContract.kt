package net.thunderbird.feature.debug.settings.featureflag

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface DebugFeatureFlagSectionContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val defaults: ImmutableMap<FeatureFlagKey, FeatureFlag> = persistentMapOf(),
        val overrides: ImmutableMap<FeatureFlagKey, Boolean> = persistentMapOf(),
        val pendingOverrides: ImmutableMap<FeatureFlagKey, Boolean> = persistentMapOf(),
    )

    sealed interface Event {
        data object RestoreDefaults : Event
        data class OnToggle(val flag: FeatureFlag) : Event
        data object ApplyChanges : Event
    }

    sealed interface Effect {
        data object RestartMainActivity : Effect
        data class NotifyPendingChanges(
            val pendingOverrides: ImmutableMap<FeatureFlagKey, Boolean> = persistentMapOf(),
        ) : Effect
    }
}
