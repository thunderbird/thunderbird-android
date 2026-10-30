package net.thunderbird.benchmarks.common.account.setup.step

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.k9mail.feature.onboarding.welcome.ui.OnboardingWelcomeTestTags
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.extension.waitFor

internal class WelcomeStep : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        device.waitFor(By.res(OnboardingWelcomeTestTags.TEST_TAG_BTN_WELCOME_GET_STARTED)).click()
    }
}
