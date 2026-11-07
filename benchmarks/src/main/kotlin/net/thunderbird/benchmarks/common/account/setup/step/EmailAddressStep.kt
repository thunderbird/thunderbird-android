package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.UiDevice
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.extension.waitForEditTexts

/**
 * Enters the email address on the auto discovery screen.
 */
internal class EmailAddressStep : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        device.waitForEditTexts(count = 1).single().text = account.email
        device.clickNext()
    }
}
