package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.usecase.ValidateDisplayName.ValidateDisplayNameError
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import org.junit.Test

class ValidateDisplayNameTest {

    private val testSubject = ValidateDisplayName()

    @Test
    fun `should succeed when display name is set`() {
        val result = testSubject.execute("display name")

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `should fail when display name is empty`() {
        val result = testSubject.execute("")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateDisplayNameError.EmptyDisplayName>()
    }

    @Test
    fun `should fail when display name is blank`() {
        val result = testSubject.execute(" ")

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<ValidateDisplayNameError.EmptyDisplayName>()
    }
}
