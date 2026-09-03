package net.thunderbird.piisafe.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.testing.compileWithPiiLoggingPlugin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class PiiLoggingCompilerPluginRegistrarTest {
    @Test
    fun `registrar compiles a HasPii-annotated class without diagnostics`() {
        val source = """
            import net.thunderbird.core.logging.LoggingPii

            @LoggingPii.HasPii
            class Contact(val email: String)
        """.trimIndent()

        val result = compileWithPiiLoggingPlugin(
            fileName = "Contact.kt",
            source = source,
            registrar = PiiLoggingCompilerPluginRegistrar(),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}
