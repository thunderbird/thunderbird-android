package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountSetupTestTags
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.SERVER_TIMEOUT_MS
import net.thunderbird.benchmarks.common.account.setup.interaction.clickNext
import net.thunderbird.benchmarks.common.extension.waitFor
import net.thunderbird.benchmarks.common.extension.waitForAny

/**
 * Handles the auto discovery result. Only runs when the account has server settings.
 *
 * If no configuration was found, it clicks Next to go to the manual setup. If one was found, it clicks "Edit
 * configuration", after expanding the result if the button isn't visible yet.
 */
internal class AutoDiscoveryResultStep : AccountSetupStep {
    override fun isApplicable(account: BenchmarkAccount) = account.servers != null

    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        val configurationNotFound = By.res(AccountSetupTestTags.TEST_TAG_TXT_CONFIG_NOT_FOUND)
        val configurationFound = By.res(AccountSetupTestTags.TEST_TAG_TXT_CONFIG_FOUND)
        if (device.waitForAny(SERVER_TIMEOUT_MS, configurationNotFound, configurationFound) == configurationFound) {
            val editConfiguration = By.res(AccountSetupTestTags.TEST_TAG_BTN_EDIT_CONFIG)
            if (!device.hasObject(editConfiguration)) {
                device.waitFor(configurationFound).click()
            }
            device.waitFor(editConfiguration).click()
        } else {
            device.clickNext()
        }
    }
}
