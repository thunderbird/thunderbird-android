package net.thunderbird.piisafe.compiler.fir.checkers

import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.assertEquals
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
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = result.exitCode)
        assertEquals(expected = 1, actual = result.diagnosticMessages.size)
        assertEquals(expected = expectedMessage, actual = result.diagnosticMessages.first().message)
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
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
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
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
    }
}
