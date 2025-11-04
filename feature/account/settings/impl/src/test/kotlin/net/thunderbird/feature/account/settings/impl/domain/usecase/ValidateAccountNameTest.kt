package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError

class ValidateAccountNameTest {

    private val testSubject = ValidateAccountName()

    @Test
    fun `should succeed when name is valid`() {
        val result = testSubject("Valid Name")

        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when name is empty`() {
        val result = testSubject("")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAccountName.ValidateAccountNameError.EmptyName>()
    }

    @Test
    fun `should fail when name is blank`() {
        val result = testSubject(" ")

        assertThat(result).isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf<ValidateAccountName.ValidateAccountNameError.EmptyName>()
    }
}
