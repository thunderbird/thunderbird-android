package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.annotation.PII_SAFE_PLUGIN_ID
import net.thunderbird.piisafe.annotation.PiiLoggingCliOption
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
internal class PiiLoggingCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PII_SAFE_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = PiiLoggingCliOption.ENABLED.optionName,
            valueDescription = "true|false",
            description = "Enables the PII logging compiler plugin for this compilation",
            required = false,
        ),
    )

    @OptIn(CompilerConfiguration.Internals::class)
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            PiiLoggingCliOption.ENABLED.optionName -> configuration.put(
                PiiLoggingConfigurationKeys.ENABLED,
                value.toBoolean(),
            )
        }
    }
}
