package net.thunderbird.piisafe.compiler

import net.thunderbird.piisafe.annotation.options.PiiSafeCliOption
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object PiiSafeConfigurationKeys {
    val ENABLED: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create(PiiSafeCliOption.ENABLED.optionName)
}
