package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError

class ValidateAccountNameTest {

    private val testSubject = ValidateAccountName()

    @Test
    fun `should succeed when name is valid`() {
        // Act
        val result = testSubject("Valid Name")

        // Assert
        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
    }

    @Test
    fun `should fail when name is empty`() {
        // Act
        val result = testSubject("")

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<ValidateAccountNameError>>()
            .prop(Outcome.Failure<ValidateAccountNameError>::error)
            .isInstanceOf<ValidateAccountNameError.EmptyName>()
    }

    @Test
    fun `should fail when name is blank`() {
        // Act
        val result = testSubject(" ")

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<ValidateAccountNameError>>()
            .prop(Outcome.Failure<ValidateAccountNameError>::error)
            .isInstanceOf<ValidateAccountNameError.EmptyName>()
    }

    @Test
    fun `should fail when name is too long`() {
        // Arrange
        val longName = "a".repeat(255)

        // Act
        val result = testSubject(longName)

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<ValidateAccountNameError>>()
            .prop(Outcome.Failure<ValidateAccountNameError>::error)
            .isInstanceOf<ValidateAccountNameError.TooLongName>()
    }
}
