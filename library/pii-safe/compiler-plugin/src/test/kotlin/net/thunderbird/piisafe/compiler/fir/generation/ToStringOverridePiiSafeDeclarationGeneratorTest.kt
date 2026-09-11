package net.thunderbird.piisafe.compiler.fir.generation

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.fir.checkers.PiiSafeFirCheckers
import net.thunderbird.piisafe.compiler.testing.compileWithPiiSafePlugin
import net.thunderbird.piisafe.compiler.testing.testFirRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class)
class ToStringOverridePiiSafeDeclarationGeneratorTest {
    @Test
    fun `declaration generation extension compiles a trivial class without diagnostics`() {
        val firExtensionRegistrar = object : FirExtensionRegistrar() {
            override fun ExtensionRegistrarContext.configurePlugin() {
                +::PiiSafeFirCheckers
                +::ToStringOverridePiiSafeDeclarationGenerator
            }
        }
        val result = compileWithPiiSafePlugin(
            "Plain.kt",
            "class Plain(val value: String)",
            testFirRegistrar(firExtensionRegistrar),
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
}
