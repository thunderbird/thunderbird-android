package net.thunderbird.feature.account.avatar

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAtLeast
import kotlin.test.Test

class DefaultAvatarIconCatalogTest {

    @Test
    fun `default is contained in all`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.all()

        assertThat(result).contains(testSubject.defaultName)
    }

    @Test
    fun `catalog contains expected stable names`() {
        val testSubject = DefaultAvatarIconCatalog()

        val result = testSubject.all()

        assertThat(result).containsAtLeast(
            testSubject.defaultName,
            "person",
            "folder",
            "pets",
            "rocket",
            "spa",
        )
    }
}
