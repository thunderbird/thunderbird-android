package net.thunderbird.piisafe.compiler.fir.generation

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.tschuchort.compiletesting.KotlinCompilation
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator.Companion.TO_STRING_METHOD_NAME
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@Suppress("MaxLineLength")
@OptIn(ExperimentalCompilerApi::class)
class ToStringOverridePiiSafeDeclarationGeneratorTest {
    @Test
    fun `declaration generation extension compiles a trivial class without diagnostics`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "Plain.kt",
            source = "class Plain(val value: String)",
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `declaration generation extension overrides toString when class is annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "User.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class User(@get:PiiSafe.Mask val email: String)

                fun toStringCompilerErrorCheck(user: User): String = user.toString()
            """.trimIndent(),
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val userClass = result.classLoader.loadClass("User")
        val method = userClass.getDeclaredMethod(TO_STRING_METHOD_NAME)
        assertThat(method).transform { it.returnType }.isEqualTo(String::class.java)
    }

    @Test
    fun `declaration generation extension does not override toString on a class without HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "PlainUser.kt",
            source = "class PlainUser(val name: String)",
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val plainUserClass = result.classLoader.loadClass("PlainUser")
        assertFailsWith<NoSuchMethodException> {
            plainUserClass.getDeclaredMethod(TO_STRING_METHOD_NAME)
        }
    }

    @Test
    fun `declaration generation extension only overrides toString on the HasPii-annotated class among several`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "Mixed.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class Contact(@get:PiiSafe.Mask val email: String, val avatar: Avatar)
                class Avatar(val url: String)
            """.trimIndent(),
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.classLoader.loadClass("Contact").getDeclaredMethod(TO_STRING_METHOD_NAME))
            .isNotNull()
        assertFailsWith<NoSuchMethodException> {
            result.classLoader.loadClass("Avatar").getDeclaredMethod(TO_STRING_METHOD_NAME)
        }
    }

    @Test
    fun `declaration generation extension does not override toString on a non-data class annotated with HasPii`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "NonDataUser.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                class NonDataUser(@get:PiiSafe.Mask val email: String)
            """.trimIndent(),
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val nonDataUserClass = result.classLoader.loadClass("NonDataUser")
        assertFailsWith<NoSuchMethodException> {
            nonDataUserClass.getDeclaredMethod(TO_STRING_METHOD_NAME)
        }
    }

    @Test
    fun `generated toString override takes no parameters and is public`() {
        // Arrange
        val firExtensionRegistrar = TestFirExtensionRegistrar(::ToStringOverridePiiSafeDeclarationGenerator)

        // Act
        val result = compileWithPiiSafePlugin(
            fileName = "Message.kt",
            source = """
                import net.thunderbird.piisafe.annotation.PiiSafe

                @PiiSafe.HasPii
                data class Message(@get:PiiSafe.Mask val id: Int)
            """.trimIndent(),
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val method = result.classLoader.loadClass("Message").getDeclaredMethod(TO_STRING_METHOD_NAME)
        assertThat(method).all {
            transform { it.parameterCount }.isEqualTo(0)
            transform { Modifier.isPublic(it.modifiers) }.isTrue()
        }
    }
}
