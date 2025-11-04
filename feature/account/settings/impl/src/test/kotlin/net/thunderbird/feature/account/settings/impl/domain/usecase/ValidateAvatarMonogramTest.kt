package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError

class ValidateAvatarMonogramTest {

    private val testSubject = ValidateAvatarMonogram()

    @Test
    fun `should succeed when monogram length is between 1 and 3`() {
        assertThat(testSubject("A")).isInstanceOf<Outcome.Success<Unit>>()
        assertThat(testSubject("AB")).isInstanceOf<Outcome.Success<Unit>>()
        assertThat(testSubject("ABC")).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when monogram is empty`() {
        val result = testSubject("")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAvatarMonogram.ValidateAvatarMonogramError.EmptyMonogram>()
    }

    @Test
    fun `should fail when monogram is blank`() {
        val result = testSubject(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAvatarMonogram.ValidateAvatarMonogramError.EmptyMonogram>()
    }

    @Test
    fun `should fail when monogram is longer than 3 characters`() {
        val result = testSubject("ABCD")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAvatarMonogram.ValidateAvatarMonogramError.TooLongMonogram>()
    }
}
