package net.thunderbird.app.common

import android.os.Bundle
import com.fsck.k9.ui.base.BaseActivity
import kotlin.getValue
import net.thunderbird.app.common.startup.StartupRouter
import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private val startupRouter: StartupRouter by inject()
    private val databaseUpgradeInterceptor: DatabaseUpgradeInterceptor by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (databaseUpgradeInterceptor.checkAndHandleUpgrade(this, intent)) {
            finish()
            return
        }

        startupRouter.routeToNextScreen(this)
        finish()
    }
}
