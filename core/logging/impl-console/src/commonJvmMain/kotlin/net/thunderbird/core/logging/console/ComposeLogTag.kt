package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.Logger

/**
 * Composes a tag for the given [LogEvent].
 *
 * If the event has a tag, it is used; otherwise, a tag is extracted from the stack trace.
 * The tag is processed using the [processTag] method before being returned.
 *
 * @receiver The [LogEvent] to compose a tag for.
 * @param ignoredClasses The set of Class full name to be ignored.
 * @param processTag Processes a tag before it is used for logging.
 * @return The composed tag, or null if no tag could be determined.
 */
internal fun LogEvent.composeTag(
    ignoredClasses: Set<String>,
    processTag: (String) -> String? = { it },
): String? {
    // If a tag is provided, use it; otherwise, extract it from the stack trace
    val rawTag = tag ?: extractTagFromStackTrace(ignoredClasses)
    // Process the tag before returning it
    return rawTag?.let { processTag(it) }
}

/**
 * Extracts a tag from the stack trace.
 *
 * @return The extracted tag, or null if no suitable tag could be found.
 */
private fun extractTagFromStackTrace(ignoredClasses: Set<String>): String? {
    // Some classes are not available to this module, and we don't want
    // to add the dependency just for class filtering.
    val ignoredClasses = ignoredClasses + setOf(
        "net.thunderbird.core.logging.console.ComposeLogTagKt",
        "net.thunderbird.core.logging.composite.DefaultCompositeLogSink",
        "net.thunderbird.core.logging.legacy.Log",
        Logger::class.java.name,
        DefaultLogger::class.java.name,
    )

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    val stackTrace = Throwable().stackTrace

    return stackTrace
        .firstOrNull { element ->
            ignoredClasses.none { element.className.startsWith(it) }
        }
        ?.let(::createStackElementTag)
}

/**
 * Creates a tag from a stack trace element.
 *
 * @param element The stack trace element to create a tag from.
 * @return The created tag.
 */
private fun createStackElementTag(element: StackTraceElement): String {
    var tag = element.className.substringAfterLast('.')
    val regex = "(\\$\\d+)+$".toRegex()
    if (regex.containsMatchIn(input = tag)) {
        tag = regex.replace(input = tag, replacement = "")
    }
    return tag
}
