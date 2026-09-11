package net.thunderbird.piisafe.compiler.ir

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator
import net.thunderbird.piisafe.compiler.testing.call
import net.thunderbird.piisafe.compiler.testing.callStatic
import net.thunderbird.piisafe.compiler.testing.captured
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.construct
import net.thunderbird.piisafe.compiler.testing.fixture
import net.thunderbird.piisafe.compiler.testing.loadClass
import net.thunderbird.piisafe.compiler.testing.testIrRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Each test compiles a fixture from `fixtures/` with the Pii-safe plugin, invokes a
 * `Logger.{verbose,debug,info,warn,error}` call through reflection and asserts the message string that
 * reaches the logger: any `@PiiSafe.HasPii`-typed value reaching the message via string-template
 * interpolation, an explicit `.toString()` call, or string concatenation must be rendered through its
 * pii data hidden accordingly with the `@PiiSafe` annotation.
 */
@OptIn(ExperimentalCompilerApi::class)
class LoggerCallToStringTest {
    @Test
    fun `ensure string-template interpolation of a HasPii value masks pii data`() {
        // Arrange
        val result = compile("Interpolation.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertEquals(expected = "User: User(name = Alice, email = <sensitive>)", actual = actual)
    }

    @Test
    fun `ensure an explicit toString call on a HasPii value masks pii data`() {
        // Arrange
        val result = compile("ExplicitToString.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertEquals(expected = "User: User(name = Alice, email = <sensitive>)", actual = actual)
    }

    @Test
    fun `ensure string concatenation of a HasPii value masks pii data`() {
        // Arrange
        val result = compile("Concatenation.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertEquals(expected = "User: User(name = Alice, email = <sensitive>)", actual = actual)
    }

    @Test
    fun `ensure that values not annotated with HasPii are unchanged`() {
        // Arrange
        val result = compile("NotPii.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val plain = result.loadClass("Plain").construct("hello")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logPlain", plain)
        val actual = logger.captured()

        // Assert
        assertEquals(expected = "Plain: Plain(value=hello)", actual = actual)
    }

    @Test
    fun `mask multiple HasPii values interpolated in the same message`() {
        // Arrange
        val result = compile("MultiplePii.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val user = result.loadClass("User").construct("Alice", "alice@example.com")
        val account = result.loadClass("Account").construct("acc-1", "123-45-6789")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logBoth", user, account)
        val actual = logger.captured()

        // Assert
        assertEquals(
            expected = "User: User(name = Alice, email = <sensitive>), " +
                "Account: Account(id = acc-1, ssn = <sensitive>)",
            actual = actual,
        )
    }

    @Test
    fun `mask only the HasPii value in a message mixing HasPii and plain values`() {
        // Arrange
        val result = compile("MixedPii.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val user = result.loadClass("User").construct("Alice", "alice@example.com")
        val plain = result.loadClass("Plain").construct("hello")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logMixed", user, plain)
        val actual = logger.captured()

        // Assert
        assertEquals(
            expected = "User: User(name = Alice, email = <sensitive>), Plain: Plain(value=hello)",
            actual = actual,
        )
    }

    @Test
    fun `mask a HasPii value nested inside a collection`() {
        // Arrange
        val result = compile("PiiInsideCollection.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = listOf(alice, jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideCollection").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertEquals(
            expected = "Users: [User(name = Alice, email = <sensitive>), User(name = Jorge, email = <sensitive>)]",
            actual = actual,
        )
    }

    @Test
    fun `mask a HasPii value nested inside an empty collection`() {
        // Arrange
        val result = compile("PiiInsideCollection.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideCollection").callStatic<Unit>("logUsers", logger, emptyList<Any>())
        val actual = logger.captured()

        // Assert
        assertEquals(expected = "Users: []", actual = actual)
    }

    @Test
    fun `mask a HasPii value nested inside a Set`() {
        // Arrange
        val result = compile("PiiInsideSet.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = linkedSetOf(alice, jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideSet").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertEquals(
            expected = "Users: [User(name = Alice, email = <sensitive>), User(name = Jorge, email = <sensitive>)]",
            actual = actual,
        )
    }

    @Test
    fun `ensure a collection of non-HasPii values are untouched`() {
        // Arrange
        val result = compile("NonPiiCollection.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("NonPiiCollection").callStatic<Unit>("logItems", logger, listOf("a", "b"))
        val actual = logger.captured()

        // Assert
        assertEquals(expected = "Items: [a, b]", actual = actual)
    }

    @Test
    fun `mask a HasPii value nested inside a Map`() {
        // Arrange
        val result = compile("PiiInsideMap.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = linkedMapOf("alice" to alice, "jorge" to jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideMap").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertEquals(
            expected = "Users: {alice=User(name = Alice, email = <sensitive>), " +
                "jorge=User(name = Jorge, email = <sensitive>)}",
            actual = actual,
        )
    }

    @Test
    fun `ensure a Map of non-HasPii values is untouched`() {
        // Arrange
        val result = compile("NonPiiMap.fixture.kt")
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("NonPiiMap").callStatic<Unit>("logItems", logger, linkedMapOf("a" to 1, "b" to 2))
        val actual = logger.captured()

        // Assert
        assertEquals(expected = "Items: {a=1, b=2}", actual = actual)
    }

    private fun compile(fixtureName: String): JvmCompilationResult =
        compileWithPiiSafePlugin(
            fileName = fixtureName,
            source = fixture(fixtureName),
            registrar = testIrRegistrar(
                TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator),
                ToStringOverridePiiSafeBodyGenerator(),
            ),
        )

    private fun JvmCompilationResult.invokeLogUser(): String? {
        val user = loadClass("User").construct("Alice", "alice@example.com")
        val logger = loadClass("FakeLogger").construct()
        logger.call<Unit>("logUser", user)
        return logger.captured()
    }
}
