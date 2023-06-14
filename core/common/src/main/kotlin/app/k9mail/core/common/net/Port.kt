package app.k9mail.core.common.net

@Suppress("MagicNumber")
@JvmInline
value class Port(val value: Int) {
    init {
        require(value in 1..65535) { "Not a valid port number: $value" }
    }
}

fun Int.toPort() = Port(this)
