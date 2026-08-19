package net.thunderbird.core.featureflag.provider

import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.resolveTargetingKey
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext.Value
import net.thunderbird.core.featureflag.provider.context.ImmutableFeatureFlagContext
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator

/**
 * Initializes a catalog-based feature flag provider with application context and targeting configuration.
 *
 * @param evaluator The catalog feature flag provider evaluator to initialize.
 * @param featureFlagConfigStore Configuration store containing the persistent targeting key.
 * @param app The application identifier used for feature flag targeting and build variant resolution.
 * @param buildType The build type (e.g., debug, release) used for variant-specific flag overrides.
 * @param appVersion The application version string for version-based feature flag targeting.
 * @param extras Optional additional attributes to include in the feature flag evaluation context.
 */
suspend fun initializeFeatureFlags(
    evaluator: MultiFeatureFlagProviderEvaluator,
    featureFlagConfigStore: ConfigStore<FeatureFlagConfigData>,
    app: String,
    buildType: String,
    appVersion: String,
    extras: Map<String, Value> = emptyMap(),
) {
    evaluator.initialize(
        initialContext = ImmutableFeatureFlagContext(
            targetingKey = featureFlagConfigStore.resolveTargetingKey().toString(),
            attributes = mapOf(
                "app" to Value.String(app),
                "build_type" to Value.String(buildType),
                "variant" to Value.String("$app-$buildType"),
                "app_version" to Value.String(appVersion),
            ) + extras,
        ),
    )
}
