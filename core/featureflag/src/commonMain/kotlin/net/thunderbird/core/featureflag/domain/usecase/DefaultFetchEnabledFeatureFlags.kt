package net.thunderbird.core.featureflag.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.io.IOException
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.FetchEnabledFeatureFlags
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.logging.Logger

private const val LOG_ID = "[feature-flag][usecase][fetch-enabled]"

internal class DefaultFetchEnabledFeatureFlags(
    private val logger: Logger,
    private val configStore: FeatureFlagConfigStore,
    private val featureFlagProvider: MultiFeatureFlagProviderEvaluator,
    private val remoteDataSource: FeatureFlagCatalogDataSource,
) : FetchEnabledFeatureFlags {

    override fun invoke(): Flow<Outcome<FetchEnabledFeatureFlags.Success, FetchEnabledFeatureFlags.Failure>> {
        logger.verbose { "$LOG_ID fetching enabled feature flags" }
        return combine<
            FeatureFlagConfigData,
            List<FeatureFlagKey>,
            FeatureFlagCatalog?,
            Outcome<FetchEnabledFeatureFlags.Success, FetchEnabledFeatureFlags.Failure>,
            >(
            configStore.config,
            featureFlagProvider.enabledFlags,
            remoteDataSource.observe(),
        ) { config, enabledFlags, data ->
            logger.verbose {
                """$LOG_ID combine called:
                    |  feature flag config store: $config
                    |  enabled flags: $enabledFlags
                """.trimMargin()
            }

            Outcome.success(
                FetchEnabledFeatureFlags.Success(
                    enabled = config.remoteCatalogConfig.enabled && data != null,
                    available = data != null,
                    isRuntimeOverride = config.overrides.isNotEmpty(),
                    flags = enabledFlags,
                ),
            )
        }.catch { error ->
            if (error !is IOException) throw error
            logger.error(throwable = error) { "$LOG_ID failed to read the feature flag config" }
            emit(Outcome.failure(FetchEnabledFeatureFlags.Failure.ConfigReadFailed(cause = error)))
        }
    }
}
