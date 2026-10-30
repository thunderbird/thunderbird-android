package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.extension.waitFor

/**
 * Keeps the default sync options and clicks Next.
 *
 * The screen has two dropdowns (they also show up as `EditText`) and the "Show notifications" checkbox.
 */
internal class SyncOptionsStep : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        device.waitFor(By.checkable(true))
        device.clickNext()
    }
}
