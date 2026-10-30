package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.k9mail.feature.onboarding.permissions.ui.OnboardingPermissionsTestTags
import net.thunderbird.benchmarks.common.MESSAGE_LIST_RESOURCE_ID
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.account.setup.interaction.SERVER_TIMEOUT_MS
import net.thunderbird.benchmarks.common.extension.waitFor
import net.thunderbird.benchmarks.common.extension.waitForAny

/**
 * Skips the permissions screen if it is shown, then waits for the message list.
 */
internal class PermissionsStep(private val packageName: String) : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        val messageList = By.res(packageName, MESSAGE_LIST_RESOURCE_ID)
        val permissionsSkipButton = By.res(OnboardingPermissionsTestTags.TEST_TAG_BTN_PERMISSIONS_SKIP)
        if (device.waitForAny(SERVER_TIMEOUT_MS, messageList, permissionsSkipButton) == permissionsSkipButton) {
            device.waitFor(permissionsSkipButton).click()
            device.waitFor(messageList)
        }
    }
}
