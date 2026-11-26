package net.thunderbird.piisafe.compiler.ir

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import net.thunderbird.piisafe.compiler.testing.call
import net.thunderbird.piisafe.compiler.testing.callStatic
import net.thunderbird.piisafe.compiler.testing.captured
import net.thunderbird.piisafe.compiler.testing.compile
import net.thunderbird.piisafe.compiler.testing.construct
import net.thunderbird.piisafe.compiler.testing.loadClass
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
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertThat(actual).isEqualTo("User: User(name = Alice, email = <sensitive>)")
    }

    @Test
    fun `ensure an explicit toString call on a HasPii value masks pii data`() {
        // Arrange
        val result = compile("ExplicitToString.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertThat(actual).isEqualTo("User: User(name = Alice, email = <sensitive>)")
    }

    @Test
    fun `ensure string concatenation of a HasPii value masks pii data`() {
        // Arrange
        val result = compile("Concatenation.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val actual = result.invokeLogUser()

        // Assert
        assertThat(actual).isEqualTo("User: User(name = Alice, email = <sensitive>)")
    }

    @Test
    fun `ensure that values not annotated with HasPii are unchanged`() {
        // Arrange
        val result = compile("NotPii.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val plain = result.loadClass("Plain").construct("hello")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logPlain", plain)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo("Plain: Plain(value=hello)")
    }

    @Test
    fun `mask multiple HasPii values interpolated in the same message`() {
        // Arrange
        val result = compile("MultiplePii.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val user = result.loadClass("User").construct("Alice", "alice@example.com")
        val account = result.loadClass("Account").construct("acc-1", "123-45-6789")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logBoth", user, account)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo(
            "User: User(name = Alice, email = <sensitive>), " +
                "Account: Account(id = acc-1, ssn = <sensitive>)",
        )
    }

    @Test
    fun `mask only the HasPii value in a message mixing HasPii and plain values`() {
        // Arrange
        val result = compile("MixedPii.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val user = result.loadClass("User").construct("Alice", "alice@example.com")
        val plain = result.loadClass("Plain").construct("hello")
        val logger = result.loadClass("FakeLogger").construct()
        logger.call<Unit>("logMixed", user, plain)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo("User: User(name = Alice, email = <sensitive>), Plain: Plain(value=hello)")
    }

    @Test
    fun `mask a HasPii value nested inside a collection`() {
        // Arrange
        val result = compile("PiiInsideCollection.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = listOf(alice, jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideCollection").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo(
            "Users: [User(name = Alice, email = <sensitive>), User(name = Jorge, email = <sensitive>)]",
        )
    }

    @Test
    fun `mask a HasPii value nested inside an empty collection`() {
        // Arrange
        val result = compile("PiiInsideCollection.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideCollection").callStatic<Unit>("logUsers", logger, emptyList<Any>())
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo("Users: []")
    }

    @Test
    fun `mask a HasPii value nested inside a Set`() {
        // Arrange
        val result = compile("PiiInsideSet.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = linkedSetOf(alice, jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideSet").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo(
            "Users: [User(name = Alice, email = <sensitive>), User(name = Jorge, email = <sensitive>)]",
        )
    }

    @Test
    fun `ensure a collection of non-HasPii values are untouched`() {
        // Arrange
        val result = compile("NonPiiCollection.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("NonPiiCollection").callStatic<Unit>("logItems", logger, listOf("a", "b"))
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo("Items: [a, b]")
    }

    @Test
    fun `mask a HasPii value nested inside a Map`() {
        // Arrange
        val result = compile("PiiInsideMap.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val userClass = result.loadClass("User")
        val alice = userClass.construct("Alice", "alice@example.com")
        val jorge = userClass.construct("Jorge", "jorge@example.com")
        val users = linkedMapOf("alice" to alice, "jorge" to jorge)
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("PiiInsideMap").callStatic<Unit>("logUsers", logger, users)
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo(
            "Users: {alice=User(name = Alice, email = <sensitive>), " +
                "jorge=User(name = Jorge, email = <sensitive>)}",
        )
    }

    @Test
    fun `ensure a Map of non-HasPii values is untouched`() {
        // Arrange
        val result = compile("NonPiiMap.fixture.kt")
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Act
        val logger = result.loadClass("FakeLogger").construct()
        result.loadClass("NonPiiMap").callStatic<Unit>("logItems", logger, linkedMapOf("a" to 1, "b" to 2))
        val actual = logger.captured()

        // Assert
        assertThat(actual).isEqualTo("Items: {a=1, b=2}")
    }

    private fun JvmCompilationResult.invokeLogUser(): String? {
        val user = loadClass("User").construct("Alice", "alice@example.com")
        val logger = loadClass("FakeLogger").construct()
        logger.call<Unit>("logUser", user)
        return logger.captured()
    }
}
