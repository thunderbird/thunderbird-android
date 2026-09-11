package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.compiler.fir.PiiSafeFirExtensionRegistrar
import net.thunderbird.piisafe.compiler.ir.ToStringOverridePiiSafeBodyGenerator
import net.thunderbird.piisafe.compiler.plugin.buildconfig.BuildConfig
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)
internal class PiiSafeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = BuildConfig.PII_SAFE_PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val enabled = configuration[PiiSafeConfigurationKeys.ENABLED] ?: BuildConfig.PII_SAFE_PLUGIN_ENABLED
        if (!enabled) return

        FirExtensionRegistrarAdapter.registerExtension(PiiSafeFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(ToStringOverridePiiSafeBodyGenerator())
    }
}
