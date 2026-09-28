package net.thunderbird.app.common

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.fsck.k9.ui.base.BaseActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.app.common.startup.StartupRouter
import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.logging.Logger
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private val startupRouter: StartupRouter by inject()
    private val databaseUpgradeInterceptor: DatabaseUpgradeInterceptor by inject()
    private val featureFlagProvider: MultiFeatureFlagProviderEvaluator by inject()
    private val logger: Logger by inject()
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        featureFlagProvider
            .state
            .onEach { state ->
                logger.verbose { "[feature-flag][${featureFlagProvider.metadata.name}] state = $state" }
                if (state == CatalogFeatureFlagProvider.State.Resolved) {
                    ready = true
                    if (databaseUpgradeInterceptor.checkAndHandleUpgrade(this@MainActivity, intent)) {
                        finish()
                        return@onEach
                    }

                    startupRouter.routeToNextScreen(this@MainActivity)
                    finish()
                }
            }
            .launchIn(lifecycleScope)

        splashScreen.setKeepOnScreenCondition {
            logger.verbose { "[feature-flag] keep on screen; ready = $ready" }
            !ready
        }
    }
}
