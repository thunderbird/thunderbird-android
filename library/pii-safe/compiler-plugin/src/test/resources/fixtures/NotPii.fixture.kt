import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.LogMessage
import net.thunderbird.components.core.logging.LogTag

data class Plain(val value: String)

private const val TAG = "NotPii"

class FakeLogger : Logger {
    var captured: String? = null
    override fun verbose(tag: LogTag, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun debug(tag: LogTag, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun info(tag: LogTag, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun warn(tag: LogTag, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun error(tag: LogTag, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }

    fun logPlain(plain: Plain) {
        debug(TAG) { "Plain: $plain" }
    }
}
