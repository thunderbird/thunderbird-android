package net.thunderbird.feature.account.avatar

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAtLeast
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

class ImageVectorAvatarIconCatalogTest {

    @Test
    fun `default is contained in all`() {
        val testSubject = ImageVectorAvatarIconCatalog()

        val result = testSubject.allNames()

        assertThat(result).contains(testSubject.defaultName)
    }

    @Test
    fun `catalog contains expected stable names`() {
        val testSubject = ImageVectorAvatarIconCatalog()

        val result = testSubject.allNames()

        assertThat(result).containsAtLeast(
            testSubject.defaultName,
            "person",
            "work",
            "pets",
            "rocket",
            "spa",
        )
    }

    @Test
    fun `toIcon maps known name to image vector`() {
        val testSubject = ImageVectorAvatarIconCatalog()

        val icon = testSubject.toIcon("person")

        assertThat(icon).isEqualTo(Icons.Outlined.Person)
    }

    @Test
    fun `toIcon returns default for unknown name`() {
        val testSubject = ImageVectorAvatarIconCatalog()

        val icon = testSubject.toIcon("does-not-exist")

        assertThat(icon).isEqualTo(testSubject.defaultIcon)
    }

    @Test
    fun `contains is case insensitive`() {
        val testSubject = ImageVectorAvatarIconCatalog()

        assertThat(testSubject.contains("PeRsOn")).isTrue()
    }
}
