package net.thunderbird.feature.notification.api.ui.style.builder

import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal const val MAX_LINES = 5
private const val MAX_LINES_ERROR_MESSAGE = "The maximum number of lines for a inbox notification is $MAX_LINES"

/**
 * Builder for [SystemNotificationStyle.InboxStyle].
 *
 * This style is used to display a list of items in the notification's content.
 * It is commonly used for email or messaging apps.
 */
class InboxSystemNotificationStyleBuilder internal constructor(
    private var bigContentTitle: String? = null,
    private var summary: String? = null,
    private val lines: MutableList<CharSequence> = mutableListOf(),
) {
    /**
     * Sets the title for the notification's big content view.
     *
     * This method is used to specify the main title text that will be displayed
     * when the notification is expanded to show its detailed content.
     *
     * @param title The string to be used as the big content title.
     */
    fun title(title: String) {
        bigContentTitle = title
    }

    /**
     * Sets the summary of the item.
     *
     * @param summary The summary of the item.
     */
    fun summary(summary: String) {
        this.summary = summary
    }

    /**
     * Append a line to the digest section of the Inbox notification.
     *
     * @param line The line to add.
     */
    fun line(line: CharSequence) {
        require(lines.size < MAX_LINES) { MAX_LINES_ERROR_MESSAGE }
        lines += line
    }

    /**
     * Adds one or more lines to the digest section of the Inbox notification.
     *
     * @param lines A variable number of CharSequence objects representing the lines to be added.
     */
    fun lines(vararg lines: CharSequence) {
        require(lines.size < MAX_LINES) { MAX_LINES_ERROR_MESSAGE }
        this.lines += lines
    }

    /**
     * Builds and returns a [SystemNotificationStyle.InboxStyle] object.
     *
     * This method performs checks to ensure that mandatory fields like the big content title
     * and summary are provided before creating the notification style object.
     *
     * @return A [SystemNotificationStyle.InboxStyle] object configured with the specified
     *  title, summary, and lines.
     * @throws IllegalStateException if the big content title or summary is not set.
     */
    @Suppress("VisibleForTests")
    internal fun build(): SystemNotificationStyle.InboxStyle = SystemNotificationStyle.InboxStyle(
        bigContentTitle = checkNotNull(bigContentTitle) {
            "The inbox notification's title is required"
        },
        summary = checkNotNull(summary) {
            "The inbox  notification's summary is required"
        },
        lines = lines.toList(),
    )
}
