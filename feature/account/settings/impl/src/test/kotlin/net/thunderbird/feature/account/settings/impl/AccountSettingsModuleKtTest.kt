package net.thunderbird.feature.account.settings.impl

import kotlin.test.Test
import net.thunderbird.feature.account.settings.featureAccountSettingsModule
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.check.checkKoinModules
import org.koin.test.verify.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(KoinExperimentalAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class AccountSettingsModuleKtTest : AutoCloseKoinTest() {

    private val externalModule: Module = module {
        // add external dependencies
    }

    @Test
    fun `should hava a valid di module`() {
        featureAccountSettingsModule.verify()

        checkKoinModules(
            modules = listOf(externalModule, featureAccountSettingsModule),
            appDeclaration = { androidContext(RuntimeEnvironment.getApplication()) },
        )
    }
}
