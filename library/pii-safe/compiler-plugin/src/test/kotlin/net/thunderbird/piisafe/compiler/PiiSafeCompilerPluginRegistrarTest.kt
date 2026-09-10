package net.thunderbird.piisafe.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}
