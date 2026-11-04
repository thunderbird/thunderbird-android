package net.thunderbird.piisafe.compiler.fir.checkers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
@Suppress("MaxLineLength")
class HasPiiRequiresAnnotatedPropertyCheckerTest {
    @Test
    fun `checker should report error when class is annotated with HasPii but no property is annotated with either Mask or Hide`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class User(val name: String)
        """.trimIndent()
        val expectedMessage =
            "Class annotated with @PiiSafe.HasPii must have at least one property annotated with " +
                "@PiiSafe.Hide or @PiiSafe.Mask."

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "UserPiiTest.kt",
            source = source,
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.diagnosticMessages.size).isEqualTo(1)
        assertThat(result.diagnosticMessages.first().message).isEqualTo(expectedMessage)
    }

    @Test
    fun `checker should compile OK when class is annotated with HasPii and any property is annotated with either Mask or Hide`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class User(@get:PiiSafe.Mask val email: String, @get:PiiSafe.Hide val password: String)
        """.trimIndent()

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "UserPiiTest.kt",
            source = source,
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `checker should compile OK when class is annotated with HasPii and has a property whose type is annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class Address(@get:PiiSafe.Mask val street: String)

            @PiiSafe.HasPii
            data class User(val address: Address)
        """.trimIndent()

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "UserPiiTest.kt",
            source = source,
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `checker should require Mask or Hide when HasPii type is nested in an unmasked generic`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class MessageAddress(@get:PiiSafe.Mask val value: String)

            class LeakyEnvelope<T>(val value: T) {
                override fun toString() = (value as MessageAddress).value
            }

            @PiiSafe.HasPii
            data class MessageEnvelope(val secret: LeakyEnvelope<MessageAddress>)
        """.trimIndent()
        val expectedMessage =
            "Class annotated with @PiiSafe.HasPii must have at least one property annotated with " +
                "@PiiSafe.Hide or @PiiSafe.Mask."

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "NestedPiiTypeTest.kt",
            source = source,
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.diagnosticMessages.size).isEqualTo(1)
        assertThat(result.diagnosticMessages.first().message).isEqualTo(expectedMessage)
    }
}
