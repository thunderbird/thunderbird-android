package net.thunderbird.benchmarks.common.account.setup

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.k9mail.feature.onboarding.welcome.ui.OnboardingWelcomeTestTags
import net.thunderbird.benchmarks.common.MESSAGE_LIST_RESOURCE_ID
import net.thunderbird.benchmarks.common.account.BenchmarkAccountProvider
import net.thunderbird.benchmarks.common.extension.TIMEOUT_MS
import net.thunderbird.benchmarks.common.extension.launch
import net.thunderbird.benchmarks.common.extension.waitForAny

/**
 * Starts the app and adds the account from [accountProvider] by running the [steps] in order.
 * Does nothing if an account is already set up.
 */
internal class AccountSetupRunner(
    private val packageName: String,
    private val accountProvider: BenchmarkAccountProvider,
    private val steps: List<AccountSetupStep>,
) {

    fun run(device: UiDevice) {
        val account = accountProvider.provide()
        device.launch(packageName)

        val messageList = By.res(packageName, MESSAGE_LIST_RESOURCE_ID)
        val welcomeStartButton = By.res(OnboardingWelcomeTestTags.TEST_TAG_BTN_WELCOME_GET_STARTED)
        if (device.waitForAny(TIMEOUT_MS, messageList, welcomeStartButton) == messageList) return

        steps
            .filter { it.isApplicable(account) }
            .forEach { it.execute(device, account) }
    }
}
