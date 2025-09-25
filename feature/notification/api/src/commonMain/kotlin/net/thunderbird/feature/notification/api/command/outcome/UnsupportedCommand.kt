package net.thunderbird.feature.notification.api.command.outcome

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Represents a command that cannot be executed because it is not supported in the current context.
 *
 * Typical reasons include disabled feature flags or an unknown/unsupported command on the
 * current platform or build variant.
 *
 * @param TNotification The type of notification associated with the command.
 * @property command The command that was deemed unsupported. May be null when the unsupported
 *                   state is determined before a concrete command instance is created.
 * @property reason The specific reason why the command is not supported.
 */
data class UnsupportedCommand<out TNotification : Notification>(
    override val command: NotificationCommand<out TNotification>?,
    val reason: Reason,
) : Failure<TNotification> {

    /**
     * Describes why a command is unsupported.
     */
    sealed interface Reason {
        /**
         * The command is behind a feature flag that is currently disabled.
         *
         * @property key The feature flag key that disabled this command.
         */
        data class FeatureFlagDisabled(val key: FeatureFlagKey) : Reason

        /**
         * A generic, unknown reason for an unsupported command.
         *
         * @property message A human-readable description of the issue.
         * @property throwable An optional underlying exception that provides more context.
         */
        data class Unknown(val message: String, val throwable: Throwable? = null) : Reason
    }
}
