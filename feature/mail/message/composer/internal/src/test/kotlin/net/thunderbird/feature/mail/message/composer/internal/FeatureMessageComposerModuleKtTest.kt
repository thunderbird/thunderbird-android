package net.thunderbird.feature.mail.message.composer.internal

import net.thunderbird.core.ui.theme.api.ThemeManager
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class FeatureMessageComposerModuleKtTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureMessageComposerModule.verify(
            extraTypes = listOf(
                ThemeManager::class,
            ),
        )
    }
}
