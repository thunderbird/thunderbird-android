package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.annotation.PiiLoggingCliOption
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object PiiLoggingConfigurationKeys {
    val ENABLED: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create(PiiLoggingCliOption.ENABLED.optionName)
}
