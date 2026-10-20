package net.thunderbird.app.common

import android.os.Build
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext.Value
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.featureflag.provider.initializeFeatureFlags
import org.koin.android.ext.android.inject

private const val TAG = "FeatureFlagApplication"

abstract class FeatureFlagApplication : BaseApplication() {
    protected abstract val appName: String
    protected abstract val appVersion: String

    /**
     * The build type of the app, `BuildConfig.BUILD_TYPE` from the app module.
     *
     * Don't use the `BuildConfig` of a library module. Libraries only have `debug` and `release`, so the `daily` and
     * `beta` apps would get the `release` feature flags.
     */
    protected abstract val buildType: String
    private val evaluator: MultiFeatureFlagProviderEvaluator by inject()
    private val targetingKeyStore: FeatureFlagConfigStore by inject()

    private val featureFlagScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main +
            CoroutineExceptionHandler { _, throwable ->
                logger.error(tag = TAG, throwable = throwable) { "[feature-flag] Failed to initialize Feature flags" }
            },
    )

    override fun onCreate() {
        super.onCreate()
        featureFlagScope.launch {
            initializeFeatureFlags(
                evaluator = evaluator,
                featureFlagConfigStore = targetingKeyStore,
                app = appName,
                buildType = featureFlagBuildType(buildType),
                appVersion = appVersion,
                extras = mapOf("os_sdk_int" to Value.Int(Build.VERSION.SDK_INT)),
            )
        }
    }
}
