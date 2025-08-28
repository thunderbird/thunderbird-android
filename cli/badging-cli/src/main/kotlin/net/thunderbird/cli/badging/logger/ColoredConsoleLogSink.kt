package net.thunderbird.cli.badging.logger

import net.thunderbird.cli.badging.AnsiColors.ANSI_BOLD
import net.thunderbird.cli.badging.AnsiColors.ANSI_DIM
import net.thunderbird.cli.badging.AnsiColors.ANSI_RED
import net.thunderbird.cli.badging.AnsiColors.ANSI_RESET
import net.thunderbird.cli.badging.AnsiColors.ANSI_YELLOW
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * Simple console LogSink that prints colored output for the badging CLI.
 */
class ColoredConsoleLogSink(
    override val level: LogLevel = LogLevel.VERBOSE,
) : LogSink {

    override fun log(event: LogEvent) {
        val (prefix, color) = when (event.level) {
            LogLevel.ERROR -> "ERROR:" to ANSI_RED
            LogLevel.WARN -> "WARN:" to ANSI_YELLOW
            LogLevel.INFO -> "INFO:" to null
            LogLevel.DEBUG -> "DEBUG:" to ANSI_DIM
            LogLevel.VERBOSE -> "VERBOSE:" to ANSI_DIM
        }

        val out = when (event.level) {
            LogLevel.ERROR, LogLevel.WARN -> System.err
            else -> System.out
        }

        if (color != null) {
            out.println(ANSI_BOLD + color + prefix + ANSI_RESET + " " + color + event.message + ANSI_RESET)
        } else {
            out.println(ANSI_BOLD + prefix + ANSI_RESET + " " + event.message)
        }
        event.throwable?.let { t ->
            if (event.level == LogLevel.ERROR || event.level == LogLevel.WARN) {
                t.printStackTrace(System.err)
            } else {
                t.printStackTrace(out)
            }
        }
    }
}
