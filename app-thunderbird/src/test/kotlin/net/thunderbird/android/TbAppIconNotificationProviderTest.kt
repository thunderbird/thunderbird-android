package net.thunderbird.android

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.android.provider.TbAppIconNotificationProvider

class TbAppIconNotificationProviderTest {
    @Test
    fun `provides correct Thunderbird notification icon`() {
        val provider = TbAppIconNotificationProvider()
        val icon = provider.pushNotificationIcon

        assertThat(icon)
            .isEqualTo(app.k9mail.core.ui.legacy.theme2.thunderbird.R.drawable.ic_logo_thunderbird_white)
    }
}
