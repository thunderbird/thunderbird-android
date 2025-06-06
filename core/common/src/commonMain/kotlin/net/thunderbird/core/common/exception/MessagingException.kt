package net.thunderbird.core.common.exception

open class MessagingException(
    override val message: String?,
    private val isPermanentFailure: Boolean,
    override val cause: Throwable?,
) : Exception(message, cause) {

    constructor(cause: Throwable?) : this(message = null, cause = cause, isPermanentFailure = false)
    constructor(message: String?) : this(message = message, cause = null, isPermanentFailure = false)
    constructor(message: String?, isPermanentFailure: Boolean) : this(
        message = message,
        cause = null,
        isPermanentFailure = isPermanentFailure,
    )

    constructor(message: String?, cause: Throwable?) : this(
        message = message,
        cause = cause,
        isPermanentFailure = false,
    )

    companion object {
        private const val serialVersionUID = -1
    }
}
