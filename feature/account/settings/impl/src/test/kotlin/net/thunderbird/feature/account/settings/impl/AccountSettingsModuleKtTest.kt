package net.thunderbird.feature.account.settings.impl

import kotlin.test.Test
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.featureAccountSettingsModule
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

internal class AccountSettingsModuleKtTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should hava a valid di module`() {
        featureAccountSettingsModule.verify(
            extraTypes = listOf(
                AccountId::class,
                GeneralSettingsContract.State::class,
            ),
        )
    }
}
