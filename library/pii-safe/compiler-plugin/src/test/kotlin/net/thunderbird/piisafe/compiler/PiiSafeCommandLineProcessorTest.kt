package net.thunderbird.piisafe.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)
class PiiSafeCommandLineProcessorTest {
    @Test
    fun `processOption maps enabled option into configuration`() {
        val processor = PiiSafeCommandLineProcessor()
        val configuration = CompilerConfiguration()
        val option = processor.pluginOptions.single { it.optionName == "enabled" }

        processor.processOption(option = option, value = "false", configuration = configuration)

        assertEquals(expected = false, actual = configuration[PiiSafeConfigurationKeys.ENABLED])
    }
}
