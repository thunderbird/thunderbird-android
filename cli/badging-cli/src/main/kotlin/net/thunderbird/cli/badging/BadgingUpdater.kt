package net.thunderbird.cli.badging

import java.io.File
import net.thunderbird.core.logging.Logger

class BadgingUpdater(
    private val logger: Logger,
) {
    fun update(
        goldenBadgingFile: File,
        actualBadging: String,
    ) {
        goldenBadgingFile.parentFile?.mkdirs()
        goldenBadgingFile.writeText(actualBadging)
        logger.info { "Golden badging updated at: ${goldenBadgingFile.absolutePath}" }
    }
}
