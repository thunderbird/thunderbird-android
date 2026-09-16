package net.thunderbird.piisafe.compiler.fir.checkers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import net.thunderbird.piisafe.compiler.fir.TestFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class PiiSafeFirCheckersTest {
    @Test
    fun `checkers extension compiles a trivial class without diagnostics`() {
        val firExtensionRegistrar = TestFirExtensionRegistrar(::PiiSafeFirCheckers)
        val result = compileWithPiiSafePlugin(
            fileName = "Plain.kt",
            source = "class Plain(val value: String)",
            registrar = testFirRegistrar(firExtensionRegistrar),
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}
