package net.thunderbird.core.featureflag.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore

/**
 * In-memory [FeatureFlagConfigStore]. A `null` [initialConfig] simulates a store that has not
 * persisted any configuration yet, while [readFailure] and [updateFailure] simulate storage errors.
 */
internal class FakeFeatureFlagConfigStore(
    initialConfig: FeatureFlagConfigData? = FeatureFlagConfigData.DEFAULT,
    private val readFailure: Throwable? = null,
    private val updateFailure: Throwable? = null,
) : FeatureFlagConfigStore {
    private val state = MutableStateFlow(initialConfig)

    override val config: Flow<FeatureFlagConfigData> =
        if (readFailure != null) flow { throw readFailure } else state.filterNotNull()

    fun setConfig(config: FeatureFlagConfigData) {
        state.value = config
    }

    override suspend fun update(transform: (FeatureFlagConfigData?) -> FeatureFlagConfigData) {
        if (updateFailure != null) throw updateFailure
        state.update { current -> transform(current) }
    }

    override suspend fun clear() {
        state.value = FeatureFlagConfigData.DEFAULT
    }
}
