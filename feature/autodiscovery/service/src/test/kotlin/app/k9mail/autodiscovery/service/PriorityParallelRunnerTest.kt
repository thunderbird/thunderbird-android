package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AuthenticationType.PasswordCleartext
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.autodiscovery.api.ConnectionSecurity.StartTLS
import app.k9mail.autodiscovery.api.ConnectionSecurity.TLS
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.net.toHostname
import net.thunderbird.core.common.net.toPort

@OptIn(ExperimentalCoroutinesApi::class)
class PriorityParallelRunnerTest {
    @Test
    fun `first runnable returning a success result should cancel remaining runnables`() = runTest {
        var runnableTwoStarted = false
        var runnableThreeStarted = false
        var runnableTwoCompleted = false
        var runnableThreeCompleted = false
        val runnableOne = AutoDiscoveryRunnable {
            delay(100)
            DISCOVERY_RESULT_ONE
        }
        val runnableTwo = AutoDiscoveryRunnable {
            runnableTwoStarted = true
            delay(200)
            runnableTwoCompleted = true
            DISCOVERY_RESULT_TWO
        }
        val runnableThree = AutoDiscoveryRunnable {
            runnableThreeStarted = true
            delay(200)
            runnableThreeCompleted = false
            DISCOVERY_RESULT_TWO
        }
        val runner = PriorityParallelRunner(
            runnables = listOf(runnableOne, runnableTwo, runnableThree),
            coroutineDispatcher = StandardTestDispatcher(testScheduler),
        )

        var result: AutoDiscoveryResult? = null
        launch {
            result = runner.run()
        }

        testScheduler.advanceTimeBy(50)

        assertThat(result).isNull()
        assertThat(runnableTwoStarted).isTrue()
        assertThat(runnableTwoCompleted).isFalse()
        assertThat(runnableThreeStarted).isTrue()
        assertThat(runnableThreeCompleted).isFalse()

        testScheduler.advanceUntilIdle()

        assertThat(result).isEqualTo(DISCOVERY_RESULT_ONE)
        assertThat(runnableTwoCompleted).isFalse()
        assertThat(runnableThreeCompleted).isFalse()
    }

    @Test
    fun `highest priority result should be used even if it takes longer to be produced`() = runTest {
        var runnableTwoCompleted = false
        val runnableOne = AutoDiscoveryRunnable {
            delay(100)
            DISCOVERY_RESULT_ONE
        }
        val runnableTwo = AutoDiscoveryRunnable {
            runnableTwoCompleted = true
            DISCOVERY_RESULT_TWO
        }
        val runner = PriorityParallelRunner(
            runnables = listOf(runnableOne, runnableTwo),
            coroutineDispatcher = StandardTestDispatcher(testScheduler),
        )

        var result: AutoDiscoveryResult? = null
        launch {
            result = runner.run()
        }

        testScheduler.advanceTimeBy(50)

        assertThat(result).isNull()
        assertThat(runnableTwoCompleted).isTrue()

        testScheduler.advanceUntilIdle()

        assertThat(result).isEqualTo(DISCOVERY_RESULT_ONE)
    }

    @Test
    fun `wait for higher priority runnable to complete`() = runTest {
        var runnableOneCompleted = false
        var runnableTwoCompleted = false
        val runnableOne = AutoDiscoveryRunnable {
            delay(100)
            runnableOneCompleted = true
            NO_DISCOVERY_RESULT
        }
        val runnableTwo = AutoDiscoveryRunnable {
            runnableTwoCompleted = true
            DISCOVERY_RESULT_TWO
        }
        val runner = PriorityParallelRunner(
            runnables = listOf(runnableOne, runnableTwo),
            coroutineDispatcher = StandardTestDispatcher(testScheduler),
        )

        var result: AutoDiscoveryResult? = null
        launch {
            result = runner.run()
        }

        testScheduler.advanceTimeBy(50)

        assertThat(result).isNull()
        assertThat(runnableOneCompleted).isFalse()
        assertThat(runnableTwoCompleted).isTrue()

        testScheduler.advanceTimeBy(100)

        assertThat(result).isEqualTo(DISCOVERY_RESULT_TWO)
        assertThat(runnableOneCompleted).isTrue()
    }

    companion object {
        private val NO_DISCOVERY_RESULT: AutoDiscoveryResult = NoUsableSettingsFound

        private val DISCOVERY_RESULT_ONE = AutoDiscoveryResult.Settings(
            ImapServerSettings(
                hostname = "imap.domain.example".toHostname(),
                port = 993.toPort(),
                connectionSecurity = TLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "user@domain.example",
            ),
            SmtpServerSettings(
                hostname = "smtp.domain.example".toHostname(),
                port = 587.toPort(),
                connectionSecurity = StartTLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "user@domain.example",
            ),
            isTrusted = true,
            source = "result 1",
        )

        private val DISCOVERY_RESULT_TWO = AutoDiscoveryResult.Settings(
            ImapServerSettings(
                hostname = "imap.domain.example".toHostname(),
                port = 143.toPort(),
                connectionSecurity = StartTLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "user@domain.example",
            ),
            SmtpServerSettings(
                hostname = "smtp.domain.example".toHostname(),
                port = 465.toPort(),
                connectionSecurity = TLS,
                authenticationTypes = listOf(PasswordCleartext),
                username = "user@domain.example",
            ),
            isTrusted = true,
            source = "result 2",
        )
    }
}
