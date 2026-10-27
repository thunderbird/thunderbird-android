package net.thunderbird.piisafe.compiler.testing

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.thunderbird.piisafe.compiler.plugin.buildconfig.BuildConfig
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

@OptIn(ExperimentalCompilerApi::class)
internal fun compileWithPiiSafePlugin(
    fileName: String,
    @Language("kotlin")
    source: String,
    registrar: CompilerPluginRegistrar,
): JvmCompilationResult = KotlinCompilation().apply {
    sources = listOf(SourceFile.kotlin(fileName, source))
    compilerPluginRegistrars = listOf(registrar)
    languageVersion = "2.4"
    inheritClassPath = true
}.compile()

/**
 * A minimal [CompilerPluginRegistrar] for tests that only need to exercise a [firExtensionRegistrar]
 * in isolation, without going through the real
 * [net.thunderbird.piisafe.compiler.PiiSafeCompilerPluginRegistrar].
 */
@OptIn(ExperimentalCompilerApi::class)
internal fun testFirRegistrar(firExtensionRegistrar: FirExtensionRegistrar): CompilerPluginRegistrar =
    object : CompilerPluginRegistrar() {
        override val pluginId: String = "net.thunderbird.piisafe.test"
        override val supportsK2: Boolean = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            FirExtensionRegistrarAdapter.registerExtension(firExtensionRegistrar)
        }
    }

/**
 * A minimal [CompilerPluginRegistrar] for tests that need a [firExtensionRegistrar] to synthesize the
 * FIR declaration(s) the given [irGenerationExtensions] under test then fill in with real IR, in the
 * order they're passed.
 */
@OptIn(ExperimentalCompilerApi::class)
internal fun testIrRegistrar(
    firExtensionRegistrar: FirExtensionRegistrar,
    vararg irGenerationExtensions: IrGenerationExtension,
): CompilerPluginRegistrar =
    object : CompilerPluginRegistrar() {
        override val pluginId: String = "net.thunderbird.logging.pii.test"
        override val supportsK2: Boolean = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            FirExtensionRegistrarAdapter.registerExtension(firExtensionRegistrar)
            IrGenerationExtension.registerExtension(IrDumpExtension(label = "before any extension"))
            irGenerationExtensions.forEach { IrGenerationExtension.registerExtension(it) }
            IrGenerationExtension.registerExtension(IrDumpExtension(label = "after any extension"))
        }
    }

class IrDumpExtension(private val label: String) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (BuildConfig.DUMP_IR_ON_TESTS_ENABLED) {
            println("=== IR ($label) ===")
            if (BuildConfig.DUMP_IR_ON_TESTS_KOTLIN_LIKE) {
                println(moduleFragment.dumpKotlinLike())
            } else {
                // full structural dump, like *.ir.txt
                println(moduleFragment.dump())
            }
        }
    }
}
