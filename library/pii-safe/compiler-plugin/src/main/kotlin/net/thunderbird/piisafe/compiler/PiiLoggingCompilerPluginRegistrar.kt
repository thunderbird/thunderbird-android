package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.compiler.fir.PiiLoggingFirExtensionRegistrar
import net.thunderbird.core.logging.pii.compiler.ir.LoggerCallSiteRewriter
import net.thunderbird.piisafe.compiler.ir.ToStringOverridePiiSafeBodyGenerator
import net.thunderbird.piisafe.annotation.PII_SAFE_PLUGIN_ID
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)
internal class PiiLoggingCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PII_SAFE_PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val enabled = configuration[PiiLoggingConfigurationKeys.ENABLED] ?: true
        if (!enabled) return

        FirExtensionRegistrarAdapter.registerExtension(PiiLoggingFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(ToStringOverridePiiSafeBodyGenerator())
        IrGenerationExtension.registerExtension(LoggerCallSiteRewriter())
    }
}
