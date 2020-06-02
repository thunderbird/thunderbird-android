package com.fsck.k9.ui

import com.fsck.k9.RobolectricTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class K9DrawerTest : RobolectricTest() {
    @Test
    fun testAccountColorLengthEqualsDrawerColorLength() {
        val resources = RuntimeEnvironment.application.resources

        val lightColors = resources.getIntArray(R.array.account_colors)
        val darkColors = resources.getIntArray(R.array.drawer_account_accent_color_dark_theme)

        assertEquals(lightColors.size, darkColors.size)
    }
}
