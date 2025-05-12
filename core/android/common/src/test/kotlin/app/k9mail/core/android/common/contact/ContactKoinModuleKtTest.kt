package app.k9mail.core.android.common.contact

import org.junit.Test
import org.koin.test.verify.verify

internal class ContactKoinModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        contactModule.verify()
    }
}
