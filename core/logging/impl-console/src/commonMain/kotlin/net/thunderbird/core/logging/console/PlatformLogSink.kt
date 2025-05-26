package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

internal expect fun platformLogSink(level: LogLevel): LogSink
