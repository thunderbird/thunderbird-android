package net.thunderbird.piisafe.compiler.ir

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator.Companion.TO_STRING_METHOD_NAME
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testIrRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Tests for [ToStringOverridePiiSafeBodyGenerator].
 *
 * Each test compiles a small source snippet with the Pii-safe plugin, instantiates the resulting class
 * through reflection, invokes the synthetic `toStringPiiSafe()` member and asserts the rendered string.
 *
 * Expected rendering (per class): `ClassName(prop1 = value1, prop2 = <sensitive>)`
 * - Properties annotated `@PiiSafe.Hide` are omitted entirely.
 * - Properties annotated `@PiiSafe.Mask` are rendered as `propName = <sensitive>`.
 * - All other properties are rendered as `propName = value`, with `String` values quoted.
 * - Properties are listed in declaration order, comma-separated, skipping hidden ones.
 */
@OptIn(ExperimentalCompilerApi::class)
class ToStringOverridePiiSafeBodyGeneratorTest {
    @Test
    fun `body generator compiles a class without HasPii without diagnostics`() {
        // Arrange & Act
        val result = compile(fileName = "PlainUser.kt", source = "data class PlainUser(val name: String)")

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
    }

    @Test
    fun `body generator renders a masked property as sensitive placeholder`() {
        // Arrange
        val result = compile(
            fileName = "User.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class User(@get:PiiSafe.Mask val email: String)
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe("User", String::class.java to "alice@example.com")

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(expected = """User(email = <sensitive>)""", actual = actual)
    }

    @Test
    fun `body generator renders unmasked properties with their actual value`() {
        // Arrange
        val result = compile(
            fileName = "User.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class User(val name: String, @get:PiiSafe.Mask val email: String)
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe(
            "User",
            String::class.java to "Alice",
            String::class.java to "alice@example.com",
        )

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(expected = """User(name = Alice, email = <sensitive>)""", actual = actual)
    }

    @Test
    fun `body generator omits properties annotated with Hide entirely`() {
        // Arrange
        val result = compile(
            fileName = "User.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class User(val name: String, @get:PiiSafe.Hide val password: String)
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe(
            "User",
            String::class.java to "Alice",
            String::class.java to "secret",
        )

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(expected = """User(name = Alice, +1 hidden properties)""", actual = actual)
    }

    @Test
    fun `body generator renders an empty property list when every property is Hide-annotated`() {
        // Arrange
        val result = compile(
            fileName = "User.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class User(@get:PiiSafe.Hide val user: String, @get:PiiSafe.Hide val password: String)
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe(
            "User",
            String::class.java to "usr",
            String::class.java to "secret",
        )

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(expected = "User(+2 hidden properties)", actual = actual)
    }

    @Test
    fun `body generator renders non-String unmasked properties unquoted`() {
        // Arrange
        val result = compile(
            fileName = "Message.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class Message(val id: Int, @get:PiiSafe.Mask val body: String)
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe(
            "Message",
            Int::class.javaPrimitiveType!! to 42,
            String::class.java to "hello",
        )

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(expected = """Message(id = 42, body = <sensitive>)""", actual = actual)
    }

    @Test
    fun `body generator combines plain, masked and hidden properties in declaration order`() {
        // Arrange
        val result = compile(
            fileName = "Account.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class Account(
                    val username: String,
                    @get:PiiSafe.Mask val email: String,
                    @get:PiiSafe.Hide val password: String,
                )
            """.trimIndent(),
        )

        // Act
        val actual = result.executeToStringPiiSafe(
            "Account",
            String::class.java to "alice",
            String::class.java to "alice@example.com",
            String::class.java to "secret",
        )

        // Assert
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        assertEquals(
            expected = """Account(username = alice, email = <sensitive>, +1 hidden properties)""",
            actual = actual,
        )
    }

    private fun compile(fileName: String, @Language("kotlin") source: String): JvmCompilationResult =
        compileWithPiiSafePlugin(
            fileName = fileName,
            source = source,
            registrar = testIrRegistrar(
                TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator),
                ToStringOverridePiiSafeBodyGenerator(),
            ),
        )

    private fun JvmCompilationResult.executeToStringPiiSafe(
        className: String,
        vararg args: Pair<Class<*>, Any?>,
    ): String {
        val clazz = classLoader.loadClass(className)
        val instance = clazz.getDeclaredConstructor(*args.map { it.first }.toTypedArray())
            .newInstance(*args.map { it.second }.toTypedArray())
        return clazz.getDeclaredMethod(TO_STRING_METHOD_NAME).invoke(instance) as String
    }
}
