package net.thunderbird.piisafe.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.piisafe.compiler.PiiLoggingCommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)
class PiiLoggingCommandLineProcessorTest {
    @Test
    fun `processOption maps enabled option into configuration`() {
        val processor = PiiLoggingCommandLineProcessor()
        val configuration = CompilerConfiguration()
        val option = processor.pluginOptions.single { it.optionName == "enabled" }

        processor.processOption(option = option, value = "false", configuration = configuration)

        assertEquals(expected = false, actual = configuration[PiiLoggingConfigurationKeys.ENABLED])
    }
}
