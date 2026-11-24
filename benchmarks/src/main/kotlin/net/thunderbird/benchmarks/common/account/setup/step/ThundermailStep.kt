package net.thunderbird.benchmarks.common.account.setup.step

import android.widget.EditText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import net.thunderbird.benchmarks.common.account.BenchmarkAccount
import net.thunderbird.benchmarks.common.account.setup.AccountSetupStep
import net.thunderbird.benchmarks.common.extension.TIMEOUT_MS
import net.thunderbird.benchmarks.common.extension.waitFor
import net.thunderbird.benchmarks.common.extension.waitForAny
import net.thunderbird.feature.thundermail.ui.ThundermailTestTags

/**
 * Clicks "Add an email account". Some apps don't have this screen and show the email address screen right away. Then
 * nothing is clicked.
 */
internal class ThundermailStep : AccountSetupStep {
    override fun execute(device: UiDevice, account: BenchmarkAccount) {
        val addEmailAccountButton = By.res(ThundermailTestTags.TEST_TAG_BTN_ADD_AN_EMAIL)
        val emailAddressInput = By.clazz(EditText::class.java)
        if (device.waitForAny(TIMEOUT_MS, addEmailAccountButton, emailAddressInput) == addEmailAccountButton) {
            device.waitFor(selector = addEmailAccountButton).click()
        }
    }
}
