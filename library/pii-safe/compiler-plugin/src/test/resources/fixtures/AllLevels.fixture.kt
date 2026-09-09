import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class User(val name: String, @get:PiiSafe.Mask val email: String)

class FakeLogger : Logger {
    var verboseMessage: String? = null
    var debugMessage: String? = null
    var infoMessage: String? = null
    var warnMessage: String? = null
    var errorMessage: String? = null

    override fun verbose(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        verboseMessage = message()
    }
    override fun debug(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        debugMessage = message()
    }
    override fun info(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        infoMessage = message()
    }
    override fun warn(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        warnMessage = message()
    }
    override fun error(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        errorMessage = message()
    }

    fun logUser(user: User) {
        verbose { "User: $user" }
        debug { "User: $user" }
        info { "User: $user" }
        warn { "User: $user" }
        error { "User: $user" }
    }
}
