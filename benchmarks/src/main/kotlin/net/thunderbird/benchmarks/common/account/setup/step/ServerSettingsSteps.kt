package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsTestTags
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsTestTags
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.SERVER_TIMEOUT_MS
import net.thunderbird.benchmarks.common.account.setup.interaction.acceptServerCertificateIfShown
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.extension.TIMEOUT_MS
import net.thunderbird.benchmarks.common.extension.findEditTexts
import net.thunderbird.benchmarks.common.extension.pollUntil

// Positions of the fields on the server settings screens:
// incoming: protocol, server, security, port, authentication, username, password, ...
// outgoing: server, security, port, authentication, username, password, ...
private const val INCOMING_SERVER_INDEX = 1
private const val INCOMING_USERNAME_INDEX = 5
private const val OUTGOING_SERVER_INDEX = 0
private const val OUTGOING_USERNAME_INDEX = 4

/**
 * Sets the incoming server and the password. Only runs when the account has server settings.
 */
internal class IncomingServerSettingsStep : AccountSetupStep {
    override fun isApplicable(account: BenchmarkAccount) = account.servers != null

    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        val servers = checkNotNull(account.servers)
        val fields = device.waitForServerSettings(
            screen = By.res(IncomingServerSettingsTestTags.TEST_TAG_CONTENT),
            requiredFieldCount = INCOMING_USERNAME_INDEX + 2,
        )
        fields[INCOMING_SERVER_INDEX].text = servers.incoming
        fields[INCOMING_USERNAME_INDEX + 1].text = account.password
        device.clickNext()
    }
}

/**
 * Sets the outgoing server, once the incoming server is checked. The password is copied from the
 * incoming server. Only runs when the account has server settings.
 */
internal class OutgoingServerSettingsStep : AccountSetupStep {
    override fun isApplicable(account: BenchmarkAccount) = account.servers != null

    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        val servers = checkNotNull(account.servers)
        val fields = device.waitForServerSettings(
            screen = By.res(OutgoingServerSettingsTestTags.TEST_TAG_CONTENT),
            requiredFieldCount = OUTGOING_USERNAME_INDEX + 2,
            timeoutMs = SERVER_TIMEOUT_MS,
        )
        fields[OUTGOING_SERVER_INDEX].text = servers.outgoing
        device.clickNext()
    }
}

/**
 * Waits for the server settings screen with the test tag [screen] and returns its fields once at least
 * [requiredFieldCount] of them are shown.
 */
private fun UiDevice.waitForServerSettings(
    screen: BySelector,
    requiredFieldCount: Int,
    timeoutMs: Long = TIMEOUT_MS,
): List<UiObject2> {
    val message = {
        "Timed out waiting for the server settings $screen. " +
            "If the server validation failed, check the server settings in local.properties."
    }
    return pollUntil(timeoutMs, message) {
        acceptServerCertificateIfShown()
        if (hasObject(screen)) {
            findEditTexts().takeIf { it.size >= requiredFieldCount }
        } else {
            null
        }
    }
}
