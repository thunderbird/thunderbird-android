package net.thunderbird.piisafe.compiler

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
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
        assertThat(configuration[PiiSafeConfigurationKeys.ENABLED]).isEqualTo(false)
    }
}
