package com.fsck.k9.ui

import app.k9mail.core.android.testing.RobolectricTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.size
import com.fsck.k9.core.R
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class K9DrawerTest : RobolectricTest() {
    @Test
    fun testAccountColorLengthEqualsDrawerColorLength() {
        val resources = RuntimeEnvironment.getApplication().resources

        val lightColors = resources.getIntArray(R.array.account_colors)
        val darkColors = resources.getIntArray(R.array.drawer_account_accent_color_dark_theme)

        assertThat(darkColors).size().isEqualTo(lightColors.size)
    }
}
