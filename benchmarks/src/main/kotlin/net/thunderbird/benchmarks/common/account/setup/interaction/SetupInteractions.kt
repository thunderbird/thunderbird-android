package net.thunderbird.benchmarks.common.account.setup.interaction

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.k9mail.feature.account.common.ui.AccountCommonTestTags
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateTestTags
import net.thunderbird.benchmarks.common.extension.waitFor

/**
 * Looking up and checking servers uses the network, so it can take a while.
 */
internal const val SERVER_TIMEOUT_MS = 60_000L

internal fun UiDevice.clickNext() {
    waitFor(nextButtonSelector()).click()
}

internal fun nextButtonSelector() = By.res(AccountCommonTestTags.TEST_TAG_BTN_ACCOUNT_SETUP_NEXT).enabled(true)

/**
 * Accepts the server certificate if the certificate error screen is shown, for example for a self-signed certificate.
 * "Advanced" has to be clicked first to show the details. After that, the server is checked again.
 */
internal fun UiDevice.acceptServerCertificateIfShown() {
    findObject(By.res(ServerCertificateTestTags.TEST_TAG_BTN_ADVANCED))?.click()
    findObject(By.res(ServerCertificateTestTags.TEST_TAG_BTN_ACCEPT_RISK_AND_CONTINUE))?.click()
}
