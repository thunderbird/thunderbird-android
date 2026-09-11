package net.thunderbird.piisafe.compiler

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class PiiSafeCompilerPluginRegistrarTest {
    @Test
    fun `registrar compiles a HasPii-annotated class without diagnostics`() {
        val source = """
            import net.thunderbird.piisafe.annotation.PiiSafe

            @PiiSafe.HasPii
            class Contact(val email: String)
        """.trimIndent()

        val result = compileWithPiiSafePlugin(
            fileName = "Contact.kt",
            source = source,
            registrar = PiiSafeCompilerPluginRegistrar(),
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}
