package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.SERVER_TIMEOUT_MS
import net.thunderbird.benchmarks.common.account.setup.interaction.acceptServerCertificateIfShown
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.account.setup.interaction.nextButtonSelector
import net.thunderbird.benchmarks.common.extension.findEditTexts
import net.thunderbird.benchmarks.common.extension.pollUntil

// Display options: account name, your name, signature
private const val DISPLAY_OPTIONS_FIELD_COUNT = 3
private const val DISPLAY_NAME_INDEX = 1

/**
 * Enters the display name. For IMAP, the special folders screen can come first. It is skipped if all folders were
 * found.
 */
internal class DisplayOptionsStep : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        device.waitForDisplayOptions(account.email)[DISPLAY_NAME_INDEX].text = account.displayName
        device.clickNext()
    }

    /**
     * Waits for the display options screen and returns its fields. The screen is recognized by the account name field,
     * which starts with the email address. If the special folders screen is shown instead, it clicks Next.
     */
    private fun UiDevice.waitForDisplayOptions(email: String): List<UiObject2> {
        val nextButton = nextButtonSelector()
        return pollUntil(timeoutMs = SERVER_TIMEOUT_MS, lazyMessage = { "Timed out waiting for the display options" }) {
            acceptServerCertificateIfShown()
            val editTexts = findEditTexts()
            if (editTexts.size == DISPLAY_OPTIONS_FIELD_COUNT && editTexts.first().text == email) {
                editTexts
            } else {
                // Special folders form: folder dropdowns, keep the suggested folders
                if (editTexts.isNotEmpty() && editTexts.none { it.text == email } && hasObject(nextButton)) {
                    findObject(nextButton)?.click()
                }
                null
            }
        }
    }
}
