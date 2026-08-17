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

abstract class FeatureFlagApplication : BaseApplication() {
    protected abstract val appName: String
    protected abstract val appVersion: String
    private val evaluator: MultiFeatureFlagProviderEvaluator by inject()
    private val targetingKeyStore: FeatureFlagConfigStore by inject()

    private val featureFlagScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main +
            CoroutineExceptionHandler { _, throwable ->
                logger.error(throwable = throwable) { "[feature-flag] Failed to initialize Feature flags" }
            },
    )

    override fun onCreate() {
        super.onCreate()
        featureFlagScope.launch {
            initializeFeatureFlags(
                evaluator = evaluator,
                featureFlagConfigStore = targetingKeyStore,
                app = appName,
                buildType = BuildConfig.BUILD_TYPE,
                appVersion = appVersion,
                extras = mapOf("os_sdk_int" to Value.Int(Build.VERSION.SDK_INT)),
            )
        }
    }
}
