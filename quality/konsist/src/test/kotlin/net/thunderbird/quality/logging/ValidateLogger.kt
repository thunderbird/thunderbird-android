package net.thunderbird.quality.logging

import kotlin.test.Test
import net.thunderbird.quality.util.assertNoForbiddenImports

class ValidateLogger {

    @Test
    fun `no class should use Java util logging`() {
        assertNoForbiddenImports(
            message = LOGGER_MESSAGE,
            replacementFor = { LOGGER_REPLACEMENT },
        ) { it.startsWith("java.util.logging.") }
    }

    @Test
    fun `no class should use Android util logging`() {
        assertNoForbiddenImports(
            message = LOGGER_MESSAGE,
            replacementFor = { LOGGER_REPLACEMENT },
        ) { it == "android.util.Log" }
    }

    @Test
    fun `no class should use Timber logging`() {
        assertNoForbiddenImports(
            message = LOGGER_MESSAGE,
            replacementFor = { LOGGER_REPLACEMENT },
        ) { it.startsWith("timber.log.") }
    }

    private companion object {
        const val LOGGER_REPLACEMENT = "net.thunderbird.components.core.logging.Logger"

        const val LOGGER_MESSAGE = "Inject $LOGGER_REPLACEMENT through the constructor (Koin) and call " +
            "logger.debug(TAG) { \"message\" } (also verbose, info, warn, error). " +
            "Never log PII, credentials, tokens, message content, or email addresses."
    }
}
