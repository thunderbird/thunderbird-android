package net.thunderbird.feature.account.avatar

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAtLeast
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test

class DefaultAvatarIconCatalogTest {

    @Test
    fun `default is set to person icon`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.defaultIcon

        assertThat(result).isEqualTo(DefaultAvatarIcons.Person)
    }

    @Test
    fun `default is contained in all`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.all()

        assertThat(result).contains(testSubject.defaultIcon)
    }

    @Test
    fun `all contains person icon and sets it as default`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.all()

        assertThat(result).containsAtLeast(DefaultAvatarIcons.Person)
    }

    @Test
    fun `get returns right icon`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.get("star")

        assertThat(result).isEqualTo(DefaultAvatarIcons.Star)
    }

    @Test
    fun `toIcon returns default for unknown name`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.get("does-not-exist")

        assertThat(result).isEqualTo(testSubject.defaultIcon)
    }

    @Test
    fun `get is case insensitive`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.get("StAr")

        assertThat(result).isEqualTo(DefaultAvatarIcons.Star)
    }

    @Test
    fun `contains returns false for unknown name`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.contains("does-not-exist")

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `contains returns true for known name`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.contains("person")

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `contains is case insensitive`() {
        val testSubject = DefaultAvatarIconCatalog()

        assertThat(testSubject.contains("PeRsOn")).isTrue()
    }
}
