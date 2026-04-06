package net.thunderbird.feature.mail.message.list.internal

import kotlin.test.Test
import kotlin.time.Clock
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.debugging.DebuggingSettingsPreferenceManager
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.definition
import org.koin.test.verify.verify

class FeatureMessageListModuleKtTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureMessageListModule.verify(
            extraTypes = listOf(
                Logger::class,
                Clock::class,
                StringsResourceManager::class,
                GeneralSettingsManager::class,
                DebuggingSettingsPreferenceManager::class,
            ),
            injections = listOf(
                definition<SetupArchiveFolderDialogContract.ViewModel>(SetupArchiveFolderDialogContract.State::class),
            ),
        )
    }
}
