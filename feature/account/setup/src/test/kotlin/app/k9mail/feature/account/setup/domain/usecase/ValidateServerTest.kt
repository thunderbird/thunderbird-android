package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.Test

class ValidateServerTest {

    @Test
    fun `should succeed when server is set`() {
        val useCase = ValidateServer()

        val result = useCase.execute("server")

        assertThat(result).isInstanceOf(ValidationResult.Success::class)
    }

    @Test
    fun `should fail when server is empty`() {
        val useCase = ValidateServer()

        val result = useCase.execute("")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateServer.ValidateServerError.EmptyServer::class)
    }

    @Test
    fun `should fail when server is blank`() {
        val useCase = ValidateServer()

        val result = useCase.execute(" ")

        assertThat(result).isInstanceOf(ValidationResult.Failure::class)
            .prop(ValidationResult.Failure::error)
            .isInstanceOf(ValidateServer.ValidateServerError.EmptyServer::class)
    }
}
