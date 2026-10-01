@file:JvmName("PiiInsideSet")

import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class User(val name: String, @get:PiiSafe.Mask val email: String)

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
}

fun logUsers(logger: Logger, users: Set<User>) {
    logger.debug { "Users: $users" }
}
