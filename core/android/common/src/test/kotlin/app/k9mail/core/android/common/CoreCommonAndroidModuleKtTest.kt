package app.k9mail.core.android.common

import android.content.Context
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

internal class CoreCommonAndroidModuleKtTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        coreCommonAndroidModule.verify(
            extraTypes = listOf(
                Context::class,
            ),
        )
    }
}
