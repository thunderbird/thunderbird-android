package app.k9mail.legacy.message.controller

/**
 * Interface for managing [MessagingListener] instances.
 *
 * Implementations of this registry allow components to register or unregister
 * listeners that receive messaging events, enabling decoupled event handling
 * within the messaging subsystem.
 */
interface MessagingControllerRegistry {

    fun addListener(listener: MessagingListener)

    fun removeListener(listener: MessagingListener)
}
