package net.thunderbird.piisafe.compiler.fir.checkers

import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = result.exitCode)
        assertEquals(expected = 1, actual = result.diagnosticMessages.size)
        assertEquals(expected = expectedMessage, actual = result.diagnosticMessages.first().message)
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
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
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
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
    }
}
