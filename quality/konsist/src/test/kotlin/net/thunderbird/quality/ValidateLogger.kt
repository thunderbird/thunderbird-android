package net.thunderbird.quality

import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

class ValidateLogger {

    @Test
    fun `no class should use Java util logging`() {
        projectScope.files
            .assertFalse(
                additionalMessage = "No class should use java.util.logging import, use net.thunderbird.core.logging.Logger instead."
            ) { it.hasImport { import -> import.name == "java.util.logging.." } }
    }

    @Test
    fun `no class should use Android util logging`() {
        projectScope.files
            .filterNot { it.hasNameMatching("ConsoleLogSinkTest.android".toRegex()) }
            .assertFalse(
                additionalMessage = "No class should use android.util.Log import, use net.thunderbird.core.logging.Logger instead."
            ) {
                it.hasImport { import -> import.name == "android.util.Log" }
            }
    }

    @Test
    fun `no class should use Timber logging`() {
        projectScope.files
            .filterNot { it.hasNameMatching("ConsoleLogSink.android|ConsoleLogSinkTest.android|PlatformInitializer.android".toRegex()) }
            .filterNot {
                // Exclude legacy code that still uses Timber
                it.hasNameMatching("LogFileWriter|FileLoggerTree|K9".toRegex())
            }
            .assertFalse(
                additionalMessage = "No class should use timber.log.Timber import, use net.thunderbird.core.logging.Logger instead."
            ) { it.hasImport { import -> import.name == "timber.log.Timber" } }
    }

}
