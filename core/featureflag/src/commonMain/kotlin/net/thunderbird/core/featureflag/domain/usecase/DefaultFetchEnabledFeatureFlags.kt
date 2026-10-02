package net.thunderbird.core.featureflag.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.FetchEnabledFeatureFlags
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.logging.Logger

private const val LOG_ID = "[feature-flag][usecase][fetch-enabled]"

internal class DefaultFetchEnabledFeatureFlags(
    private val logger: Logger,
    private val configStore: FeatureFlagConfigStore,
    private val featureFlagProvider: MultiFeatureFlagProviderEvaluator,
) : FetchEnabledFeatureFlags {

    override fun invoke(): Flow<Outcome<FetchEnabledFeatureFlags.Success, FetchEnabledFeatureFlags.Failure>> {
        logger.verbose { "$LOG_ID fetching enabled feature flags" }
        return combine(configStore.config, featureFlagProvider.enabledFlags) { config, enabledFlags ->
            logger.verbose {
                """$LOG_ID combine called:
                    |  feature flag config store: $config
                    |  enabled flags: $enabledFlags
                """.trimMargin()
            }

            Outcome.success(
                FetchEnabledFeatureFlags.Success(
                    enabled = config.remoteCatalogConfig.enabled,
                    isRuntimeOverride = config.overrides.isNotEmpty(),
                    flags = enabledFlags,
                ),
            )
        }
    }
}
