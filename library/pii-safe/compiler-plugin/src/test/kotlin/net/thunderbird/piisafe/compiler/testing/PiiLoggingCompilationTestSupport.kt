package net.thunderbird.piisafe.compiler.testing

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
internal fun compileWithPiiLoggingPlugin(
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
 * [net.thunderbird.piisafe.compiler.PiiLoggingCompilerPluginRegistrar].
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
