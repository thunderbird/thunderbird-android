package net.thunderbird.feature.account.avatar

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DefaultAvatarMonogramCreatorTest {

    private val testSubject = DefaultAvatarMonogramCreator()

    @Test
    fun `create returns correct monogram for name`() {
        val name = "John Doe"
        val expectedMonogram = "JO"

        val result = testSubject.create(name, null)

        assertThat(result).isEqualTo(expectedMonogram)
    }

    @Test
    fun `create returns correct monogram for email`() {
        val email = "test@example.com"
        val expectedMonogram = "TE"

        val result = testSubject.create(null, email)

        assertThat(result).isEqualTo(expectedMonogram)
    }

    @Test
    fun `create returns default monogram for null or empty inputs`() {
        val expectedMonogram = "XX"

        val resultWithNulls = testSubject.create(null, null)
        assertThat(resultWithNulls).isEqualTo(expectedMonogram)

        val resultWithEmptyStrings = testSubject.create("", "")
        assertThat(resultWithEmptyStrings).isEqualTo(expectedMonogram)
    }
}
