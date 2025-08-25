package net.thunderbird.feature.notification.api

/**
 * Represents the severity level of a notification.
 *
 * This enum is used to categorize notifications based on their importance and the urgency of the action
 * required from the user.
 * When defining an [net.thunderbird.feature.notification.api.content.AppNotification] object, consider whether user
 * action is necessary.
 *
 * For severities like [Fatal] and [Critical], user action is typically required to resolve the issue.
 * For [Temporary] and [Warning], user action might be recommended or optional.
 * For [Information], no user action is usually needed.
 */
enum class NotificationSeverity {
    /**
     * Completely blocks the user from performing essential tasks or accessing core functionality.
     *
     * **User Action:** Typically requires immediate user intervention to resolve the issue.
     *
     * **Example:**
     * - **Notification Message:** Authentication Error
     * - **Notification Actions:**
     *     - Retry
     *     - Provide other credentials
     */
    Fatal,

    /**
     * Prevents the user from completing specific core actions or causes significant disruption to functionality.
     *
     * **User Action:** Usually requires user action to fix or work around the problem.
     *
     * **Example:**
     * - **Notification Message:** Sending of the message "message subject" failed.
     * - **Notification Actions:**
     *    - Retry
     */
    Critical,

    /**
     * Causes a temporary disruption or delay to functionality, which may resolve on its own.
     *
     * **User Action:** User action might be optional or might involve waiting for the system to recover.
     * Informing the user about potential self-resolution is key.
     *
     * **Example:**
     * - **Notification Message:** You are offline, the message will be sent later.
     * - **Notification Actions:** N/A
     */
    Temporary,

    /**
     * Alerts the user to a potential issue or limitation that may affect functionality if not addressed.
     *
     * **User Action:** User action is often recommended to prevent future problems or to mitigate current limitations.
     * The action might be to adjust settings, update information, or simply be aware of a condition.
     *
     * **Example:**
     * - **Notification Message:** Your mailbox is 90% full.
     * - **Notification Actions:**
     *    - Manage Storage
     */
    Warning,

    /**
     * Provides status or context without impacting functionality or requiring action.
     *
     * **User Action:** Generally, no action is required from the user. This is purely for informational purposes.
     *
     * **Example:**
     * - **Notification Message:** Last time email synchronization succeeded
     * - **Notification Actions:** N/A
     */
    Information,
}
