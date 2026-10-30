package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.UiDevice
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.extension.waitForEditTexts

/**
 * Enters the password on the auto discovery screen. Only runs when the account has no server settings.
 */
internal class AutoDiscoveryPasswordStep : AccountSetupStep {
    override fun isApplicable(account: BenchmarkAccount) = account.servers == null

    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        device.waitForEditTexts(count = 2).last().text = account.password
        device.clickNext()
    }
}
