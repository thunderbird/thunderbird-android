package net.thunderbird.android

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.android.provider.TbAppIconNotificationProvider
import org.junit.Test
import org.mockito.Mockito.mock

class TbAppIconNotificationProviderTest {
    @Test
    fun `provides correct Thunderbird notification icon`() {
        val context = mock<Context>()
        val provider = TbAppIconNotificationProvider(context)
        val icon = provider.pushNotificationIcon

        assertThat(icon)
            .isEqualTo(app.k9mail.core.ui.legacy.theme2.thunderbird.R.drawable.ic_logo_thunderbird_white)
    }
}
