package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.ui.test.junit4.StateRestorationTester
import app.k9mail.core.ui.compose.testing.ComposeTest
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData

internal class AccountListKtTest : ComposeTest() {

    @Test
    fun `account list state should survive being saved and restored`() {
        // Arrange
        val stateRestorationTester = StateRestorationTester(composeTestRule)
        stateRestorationTester.setContent {
            ThunderbirdBoltTheme {
                AccountList(
                    accounts = persistentListOf(FakeData.MAIL_DISPLAY_ACCOUNT),
                    selectedAccount = null,
                    onAccountClick = {},
                    showStarredCount = false,
                )
            }
        }

        // Act: item keys must be storable in a Bundle, otherwise saving the list state crashes
        stateRestorationTester.emulateSavedInstanceStateRestore()
    }
}
