import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag

class FakeLogger : Logger {
    var captured: String? = null
    override fun verbose(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun debug(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun info(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun warn(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }
    override fun error(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        captured = message()
    }

    fun logGreeting(name: String) {
        debug { "Hello, $name!" }
    }
}
