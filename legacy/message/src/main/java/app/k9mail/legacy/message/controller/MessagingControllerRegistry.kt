package app.k9mail.legacy.message.controller

interface MessagingControllerRegistry {

    fun addListener(listener: MessagingListener)

    fun removeListener(listener: MessagingListener)
}
