package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.annotation.options.PiiSafeCliOption
import net.thunderbird.piisafe.compiler.plugin.buildconfig.BuildConfig
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
internal class PiiSafeCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = BuildConfig.PII_SAFE_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = PiiSafeCliOption.ENABLED.optionName,
            valueDescription = "true|false",
            description = "Enables the PII-safe compiler plugin for this compilation",
            required = false,
        ),
    )

    @OptIn(CompilerConfiguration.Internals::class)
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            PiiSafeCliOption.ENABLED.optionName ->
                configuration.put(PiiSafeConfigurationKeys.ENABLED, value.toBooleanStrict())
        }
    }
}
