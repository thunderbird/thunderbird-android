package net.thunderbird.core.featureflag.ui

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.config.BuildConfig
import net.thunderbird.core.featureflag.resources.Res
import net.thunderbird.core.featureflag.resources.feature_flag_local_location
import net.thunderbird.core.featureflag.resources.feature_flag_remote_location
import net.thunderbird.core.featureflag.resources.feature_flag_runtime_overrides
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_load_failed
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_update_failed
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import org.jetbrains.compose.resources.StringResource

interface RemoteFeatureFlagUiContract {
    abstract class ViewModel : BaseViewModel<State, Event, Effect>(initialState = State())

    data class State(
        val remoteFeatureFlagEnabled: Boolean = true,
        val remoteFeatureFlagCatalogAvailable: Boolean = false,
        val location: FeatureFlagLocation = FeatureFlagLocation.Remote,
        val flags: ImmutableList<FeatureFlagKey> = persistentListOf(),
    )

    sealed interface Event {
        data class OnEnableChange(val enabled: Boolean) : Event
    }

    sealed interface Effect {
        data class ShowError(val error: RemoteFeatureFlagError) : Effect
    }
}

sealed class FeatureFlagLocation(
    val label: StringResource,
    val url: String? = null,
) {
    data object Remote : FeatureFlagLocation(
        label = Res.string.feature_flag_remote_location,
        url = BuildConfig.FEATURE_FLAG_REMOTE_URL,
    )

    data object Local : FeatureFlagLocation(
        label = Res.string.feature_flag_local_location,
    )

    data object RuntimeOverride : FeatureFlagLocation(
        label = Res.string.feature_flag_runtime_overrides,
    )
}

sealed class RemoteFeatureFlagError(
    val message: StringResource,
) {
    /** The feature flag configuration could not be read, so the screen shows its default values. */
    data object LoadFailed : RemoteFeatureFlagError(
        message = Res.string.remote_feature_flag_screen_load_failed,
    )

    /** The remote feature flag availability could not be saved, so the previous value is kept. */
    data object UpdateFailed : RemoteFeatureFlagError(
        message = Res.string.remote_feature_flag_screen_update_failed,
    )
}
