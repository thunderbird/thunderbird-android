package net.thunderbird.core.featureflag.ui

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.FetchEnabledFeatureFlags
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.UpdateRemoteFeatureFlagAvailability

internal class RemoteFeatureFlagViewModel(
    fetchEnabledFeatureFlags: FetchEnabledFeatureFlags,
    private val updateRemoteFeatureFlagAvailability: UpdateRemoteFeatureFlagAvailability,
) : RemoteFeatureFlagUiContract.ViewModel() {
    init {
        fetchEnabledFeatureFlags()
            .map { outcome ->
                outcome.fold(
                    onSuccess = { success ->
                        updateState { current ->
                            current.copy(
                                remoteFeatureFlagEnabled = success.enabled,
                                remoteFeatureFlagCatalogAvailable = success.available,
                                location = when {
                                    success.isRuntimeOverride -> FeatureFlagLocation.RuntimeOverride
                                    success.enabled -> FeatureFlagLocation.Remote
                                    else -> FeatureFlagLocation.Local
                                },
                                flags = success.flags.toPersistentList(),
                            )
                        }
                    },
                    onFailure = { failure ->
                        when (failure) {
                            is FetchEnabledFeatureFlags.Failure.ConfigReadFailed -> emitEffect(
                                RemoteFeatureFlagUiContract.Effect.ShowError(RemoteFeatureFlagError.LoadFailed),
                            )
                        }
                    },
                )
            }
            .launchIn(viewModelScope)
    }

    override fun event(event: RemoteFeatureFlagUiContract.Event) {
        when (event) {
            is RemoteFeatureFlagUiContract.Event.OnEnableChange -> {
                val new = event.enabled
                viewModelScope.launch {
                    updateRemoteFeatureFlagAvailability(enabled = new)
                        .fold(
                            onSuccess = {
                                updateState {
                                    it.copy(remoteFeatureFlagEnabled = new)
                                }
                            },
                            onFailure = { failure ->
                                when (failure) {
                                    is UpdateRemoteFeatureFlagAvailability.Failure.ConfigUpdateFailed -> emitEffect(
                                        RemoteFeatureFlagUiContract.Effect.ShowError(
                                            RemoteFeatureFlagError.UpdateFailed,
                                        ),
                                    )
                                }
                            },
                        )
                }
            }
        }
    }
}
