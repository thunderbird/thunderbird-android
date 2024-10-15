package app.k9mail.feature.account.setup

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import org.junit.After
import org.junit.Before
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

abstract class AccountSetupComposeTest : ComposeTest() {
    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<BrandNameProvider> {
                        object : BrandNameProvider {
                            override val brandName = "BrandName"
                        }
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // This works around a bug in Koin. The method can probably be removed once we update to Koin 4.x
    // See https://github.com/InsertKoinIO/koin/issues/1900
    fun setContentWithTheme(content: @Composable () -> Unit) = composeTestRule.setContent {
        KoinContext {
            K9MailTheme2 {
                content()
            }
        }
    }
}
