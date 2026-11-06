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
    fun `create returns correct monogram for single letter name`() {
        val name = "A"
        val expectedMonogram = "A"

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

    @Test
    fun `create returns full ZWJ compound emoji for account name starting with ZWJ emoji`() {
        val phoenix = "🐦\u200D🔥"
        val expectedMonogram = phoenix

        val result = testSubject.create(phoenix, null)

        assertThat(result).isEqualTo(expectedMonogram)
    }

    @Test
    fun `create returns single full emoji when account name has emoji followed by text`() {
        val nameWithEmoji = "🐦\u200D🔥 Work"
        val expectedMonogram = "🐦\u200D🔥"

        val result = testSubject.create(nameWithEmoji, null)

        assertThat(result).isEqualTo(expectedMonogram)
    }

    @Test
    fun `create returns full compound emoji for technologist ZWJ sequence`() {
        val technologist = "👨\u200D💻"
        val expectedMonogram = technologist

        val result = testSubject.create(technologist, null)

        assertThat(result).isEqualTo(expectedMonogram)
    }

    @Test
    fun `create returns full flag emoji for regional indicator sequence`() {
        val flag = "🇮🇳"
        val expectedMonogram = flag

        val result = testSubject.create(flag, null)

        assertThat(result).isEqualTo(expectedMonogram)
    }
}
