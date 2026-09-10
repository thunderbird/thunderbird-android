package net.thunderbird.piisafe.compiler.fir.checkers

import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class)
class PiiSafeFirCheckersTest {
    @Test
    fun `checkers extension compiles a trivial class without diagnostics`() {
        val firExtensionRegistrar = object : FirExtensionRegistrar() {
            override fun ExtensionRegistrarContext.configurePlugin() {
                +::PiiSafeFirCheckers
            }
        }
        val result = compileWithPiiSafePlugin(
            fileName = "Plain.kt",
            source = "class Plain(val value: String)",
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
    }
}
