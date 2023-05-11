package app.k9mail.feature.account.setup

import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AccountSetupModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        koinApplication {
            modules(featureAccountSetupModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
