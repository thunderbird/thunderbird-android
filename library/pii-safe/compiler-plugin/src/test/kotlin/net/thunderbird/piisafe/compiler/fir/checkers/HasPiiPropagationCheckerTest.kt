package net.thunderbird.piisafe.compiler.fir.checkers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class HasPiiPropagationCheckerTest {
    @Test
    fun `checker reports error when class not annotated with HasPii has property annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class User(@get:PiiSafe.Mask val name: String)

            data class NotAnnotatedPiiClass(val user: User)
        """.trimIndent()
        val expectedMessage = "This class holds a property of a type annotated @PiiSafe.HasPii; this class " +
            "must also be annotated @PiiSafe.HasPii."

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
    fun `checker reports error when HasPii type is nested in generic type arguments`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class MessageAddress(@get:PiiSafe.Mask val value: String)

            data class MessageEnvelope(val from: Map<String, List<MessageAddress>>)
        """.trimIndent()
        val expectedMessage = "This class holds a property of a type annotated @PiiSafe.HasPii; this class " +
            "must also be annotated @PiiSafe.HasPii."

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

    @Test
    fun `checker compile successful when class annotated with HasPii has property annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            data class User(@get:PiiSafe.Mask val name: String)

            @PiiSafe.HasPii
            data class NotAnnotatedPiiClass(@get:PiiSafe.Mask val user: User)
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

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `checker compile successful when class not annotated with HasPii has property not annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val source = """
            data class User(val name: String)
            data class NotAnnotatedPiiClass(val user: User)
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
}
