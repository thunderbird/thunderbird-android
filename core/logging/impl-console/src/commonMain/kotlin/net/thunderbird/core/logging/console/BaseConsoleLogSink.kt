package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * An abstract base class for console log sinks that provides common functionality.
 *
 * This class handles tag extraction from stack traces and other common operations.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 */
abstract class BaseConsoleLogSink(
    override val level: LogLevel,
) : LogSink {

    /**
     * Logs a [LogEvent].
     *
     * @param event The [LogEvent] to log.
     */
    override fun log(event: LogEvent) {
        val tag = composeTag(event)
        logWithTag(event, tag)
    }

    /**
     * Logs a [LogEvent] with the given tag.
     *
     * @param event The [LogEvent] to log.
     * @param tag The tag to use for logging.
     */
    protected abstract fun logWithTag(event: LogEvent, tag: String?)

    /**
     * Composes a tag for the given [LogEvent].
     *
     * If the event has a tag, it is used; otherwise, a tag is extracted from the stack trace.
     * The tag is processed using the [processTag] method before being returned.
     *
     * @param event The [LogEvent] to compose a tag for.
     * @return The composed tag, or null if no tag could be determined.
     */
    protected fun composeTag(event: LogEvent): String? {
        // If a tag is provided, use it; otherwise, extract it from the stack trace
        val rawTag = event.tag ?: extractTagFromStackTrace()
        // Process the tag before returning it
        return rawTag?.let { processTag(it) }
    }

    /**
     * Extracts a tag from the stack trace.
     *
     * @return The extracted tag, or null if no suitable tag could be found.
     */
    protected fun extractTagFromStackTrace(): String? {
        return Throwable().stackTrace
            .firstOrNull { it.className !in getIgnoreClasses() }
            ?.let(::createStackElementTag)
    }

    /**
     * Creates a tag from a stack trace element.
     *
     * @param element The stack trace element to create a tag from.
     * @return The created tag.
     */
    protected fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className.substringAfterLast('.')
        val matcher = getAnonymousClassPattern().matcher(tag)
        if (matcher.find()) {
            tag = matcher.replaceAll("")
        }
        return processTag(tag)
    }

    /**
     * Processes a tag before it is used for logging.
     *
     * This method can be overridden by subclasses to perform platform-specific tag processing.
     *
     * @param tag The tag to process.
     * @return The processed tag.
     */
    protected open fun processTag(tag: String): String {
        return tag
    }

    /**
     * Gets the pattern used to identify anonymous classes in class names.
     *
     * @return The pattern for anonymous classes.
     */
    protected abstract fun getAnonymousClassPattern(): java.util.regex.Pattern

    /**
     * Gets the set of class names to ignore when extracting tags from stack traces.
     *
     * @return The set of class names to ignore.
     */
    protected abstract fun getIgnoreClasses(): Set<String>
}
