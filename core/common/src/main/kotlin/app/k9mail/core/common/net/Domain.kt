package app.k9mail.core.common.net

@JvmInline
value class Domain(val value: String) {
    init {
        requireNotNull(HostNameUtils.isLegalHostName(value)) { "Not a valid domain name: '$value'" }
    }
}

fun String.toDomain() = Domain(this)

@Suppress("SwallowedException")
fun String.toDomainOrNull(): Domain? {
    return try {
        toDomain()
    } catch (e: IllegalArgumentException) {
        null
    }
}
