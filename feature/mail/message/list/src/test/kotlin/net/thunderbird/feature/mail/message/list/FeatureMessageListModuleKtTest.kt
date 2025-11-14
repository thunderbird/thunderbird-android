package net.thunderbird.feature.mail.message.list

import kotlin.test.Test
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.GeneralSettingsManager
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
                StringsResourceManager::class,
                GeneralSettingsManager::class,
            ),
            injections = listOf(
                definition<SetupArchiveFolderDialogContract.ViewModel>(SetupArchiveFolderDialogContract.State::class),
            ),
        )
    }
}
