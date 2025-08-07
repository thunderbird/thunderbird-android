package app.k9mail

import android.content.Context
import app.k9mail.provider.K9AppNotificationIconProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.mockito.Mockito.mock

class K9AppNotificationIconProviderTest {
    @Test
    fun `provides correct K9 notification icon`() {
        val context = mock<Context>()
        val provider = K9AppNotificationIconProvider(context)
        val icon = provider.pushNotificationIcon

        assertThat(icon)
            .isEqualTo(app.k9mail.core.ui.legacy.theme2.k9mail.R.drawable.ic_logo_k9_white)
    }
}
