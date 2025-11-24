package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount

class DisplayAccountUtilsTest {

    @Test
    fun `unified account uses group icon by default`() {
        // Arrange
        val unified = UnifiedDisplayAccount(
            unreadMessageCount = 0,
            starredMessageCount = 0,
            hasError = false,
        )

        // Act
        val avatar = getDisplayAccountAvatar(unified)

        // Assert
        assertThat(avatar).isEqualTo(Avatar.Icon("group"))
    }

    @Test
    fun `mail account returns its provided avatar`() {
        // Arrange
        val providedAvatar = Avatar.Icon(name = "star")
        val mail = MailDisplayAccount(
            id = "id-1",
            name = "Account",
            email = "user@example.com",
            color = 0x123456,
            avatar = providedAvatar,
            unreadMessageCount = 5,
            starredMessageCount = 1,
            hasError = false,
        )

        // Act
        val avatar = getDisplayAccountAvatar(mail)

        // Assert
        assertThat(avatar).isEqualTo(providedAvatar)
    }
}
