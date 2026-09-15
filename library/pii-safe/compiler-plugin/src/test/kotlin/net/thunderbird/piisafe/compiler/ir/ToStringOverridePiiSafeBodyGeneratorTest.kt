package net.thunderbird.piisafe.compiler.ir

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator.Companion.TO_STRING_METHOD_NAME
import net.thunderbird.piisafe.compiler.testing.compile
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testIrRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Tests for [ToStringOverridePiiSafeBodyGenerator].
 *
 * Each test compiles a small source snippet with the Pii-safe plugin, instantiates the resulting class
 * through reflection, invokes the synthetic `toString()` member and asserts the rendered string.
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("User(email = <sensitive>)")
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("User(name = Alice, email = <sensitive>)")
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("User(name = Alice, +1 hidden properties)")
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("User(+2 hidden properties)")
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("Message(id = 42, body = <sensitive>)")
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("Account(username = alice, email = <sensitive>, +1 hidden properties)")
    }

    @Test
    fun `body generator should include only primary constructor properties in the synthetic toString`() {
        // Arrange
        val result = compile(fixtureName = "PrimaryConstructorPropertiesOnly.fixture.kt")

        // Act
        val actual = result.executeToStringPiiSafe(
            "Session",
            String::class.java to "sensitive@data.com",
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("Session(email = <sensitive>)")
    }

    @Test
    fun `body generator should mask arrays maps and collections when they are annotated with Mask`() {
        // Arrange
        val result = compile(fixtureName = "ArraysMask.fixture.kt")

        // Act
        val actual = result.executeToStringPiiSafe(
            "ArraysMask",
            List::class.java to listOf("string-1", "string-2"),
            Map::class.java to mapOf("key-1" to "value", "key-2" to "value"),
            Array<Any>::class.java to arrayOf("value-1", "value-2", "value-3"),
            ByteArray::class.java to byteArrayOf(1, 2, 3, 4, 5, 6),
            ShortArray::class.java to shortArrayOf(1, 2, 3, 4, 5, 6),
            IntArray::class.java to intArrayOf(1, 2, 3, 4, 5, 6),
            LongArray::class.java to longArrayOf(1, 2, 3, 4, 5, 6),
            FloatArray::class.java to floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f),
            DoubleArray::class.java to doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual)
            .isEqualTo(
                "ArraysMask(list = <sensitive>, map = <sensitive>, array = <sensitive>, " +
                    "byteArray = <sensitive>, shortArray = <sensitive>, intArray = <sensitive>, " +
                    "longArray = <sensitive>, floatArray = <sensitive>, doubleArray = <sensitive>)",
            )
    }

    @Test
    fun `body generator should hide arrays maps and collections when they are annotated with Hide`() {
        // Arrange
        val result = compile(fixtureName = "ArraysHide.fixture.kt")

        // Act
        val actual = result.executeToStringPiiSafe(
            "ArraysHide",
            List::class.java to listOf("string-1", "string-2"),
            Map::class.java to mapOf("key-1" to "value", "key-2" to "value"),
            Array<Any>::class.java to arrayOf("value-1", "value-2", "value-3"),
            ByteArray::class.java to byteArrayOf(1, 2, 3, 4, 5, 6),
            ShortArray::class.java to shortArrayOf(1, 2, 3, 4, 5, 6),
            IntArray::class.java to intArrayOf(1, 2, 3, 4, 5, 6),
            LongArray::class.java to longArrayOf(1, 2, 3, 4, 5, 6),
            FloatArray::class.java to floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f),
            DoubleArray::class.java to doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo("ArraysHide(+9 hidden properties)")
    }

    @Test
    fun `body generator should preserve ByteArray toString behaviour when it isn't Masked`() {
        // Arrange
        val result = compile(fixtureName = "PiiPlainArraysData.fixture.kt")

        // Act
        val actual = result.executeToStringPiiSafe(
            className = "PiiPlainArraysData",
            String::class.java to "secret string",
            Array::class.java to arrayOf(1, 2, 3),
            ByteArray::class.java to byteArrayOf(1, 2, 3),
            ShortArray::class.java to shortArrayOf(1, 2, 3),
            IntArray::class.java to intArrayOf(1, 2, 3),
            LongArray::class.java to longArrayOf(1, 2, 3),
            FloatArray::class.java to floatArrayOf(1f, 2f, 3f),
            DoubleArray::class.java to doubleArrayOf(1.0, 2.0, 3.0),
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(actual).isEqualTo(
            "PiiPlainArraysData(label = <sensitive>, array = [1, 2, 3], bytes = [1, 2, 3], " +
                "shorts = [1, 2, 3], ints = [1, 2, 3], longs = [1, 2, 3], floats = [1.0, 2.0, 3.0], " +
                "doubles = [1.0, 2.0, 3.0])",
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
