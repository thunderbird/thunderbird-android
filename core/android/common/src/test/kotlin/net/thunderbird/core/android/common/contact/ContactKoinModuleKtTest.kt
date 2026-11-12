package net.thunderbird.core.android.common.contact

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

internal class ContactKoinModuleKtTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        contactDiModule.verify()
    }
}
