package net.thunderbird.feature.account.storage.legacy

import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class AccountStorageLegacyModuleKtTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureAccountStorageLegacyModule.verify()
    }
}
