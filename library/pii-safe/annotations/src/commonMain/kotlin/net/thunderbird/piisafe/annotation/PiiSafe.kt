package net.thunderbird.piisafe.annotation

/**
 * Annotations for controlling how Personally Identifiable Information (PII) is handled in data classes.
 */
public object PiiSafe {
    /**
     * Marks a class as containing Personally Identifiable Information (PII).
     *
     * This annotation is used to identify classes that contain sensitive user data
     * which should be handled with appropriate care, such as being excluded from
     * logging or requiring special sanitization before being logged.
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS)
    public annotation class HasPii

    /**
     * Annotation to mark log tags that should be excluded from logging.
     *
     * When applied to an object property's getter, it indicates that log messages with
     * this object will hide the property behind a "+x hidden properties".
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY_GETTER)
    public annotation class Hide

    /**
     * Marks a property or parameter to indicate that its value should be masked or redacted in log output.
     *
     * When applied to an object property's getter, it indicates that log messages with
     * this object will mask the property behind a "<sensitive>" text.
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY_GETTER)
    public annotation class Mask
}
