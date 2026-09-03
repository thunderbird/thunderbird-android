package net.thunderbird.core.logging

/**
 * Annotations for controlling how Personally Identifiable Information (PII) is handled in logs.
 *
 * This object contains annotations that can be applied to properties or fields to specify
 * how sensitive data should be treated when logging.
 */
object LoggingPii {
    /**
     * Marks a class as containing Personally Identifiable Information (PII).
     *
     * This annotation is used to identify classes that contain sensitive user data
     * which should be handled with appropriate care, such as being excluded from
     * logging or requiring special sanitization before being logged.
     *
     * @property loggingEnabled If `false`, the PII logging compiler plugin will show a FIR
     *   error when it is being used in a logging message.
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS)
    annotation class HasPii(val loggingEnabled: Boolean = true)

    /**
     * Annotation to mark log tags that should be excluded from logging.
     *
     * When applied to a log tag definition, it indicates that log messages with this tag
     * should be ignored by the logging system.
     *
     * TODO(#11493): The logging compiler plugin will automatically remove it from the
     *  `toStringPIISafe()` method
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY_GETTER)
    annotation class Ignore

    /**
     * Marks a property or parameter to indicate that its value should be masked or redacted in log output.
     *
     * This annotation is used to identify sensitive data that should not be logged in plain text,
     * such as passwords, tokens, or other confidential information.
     *
     * TODO(#11493): The logging compiler plugin will automatically mask it with `<sensitive>` in the
     *  `toStringPIISafe()` method
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY_GETTER)
    annotation class Mask
}
