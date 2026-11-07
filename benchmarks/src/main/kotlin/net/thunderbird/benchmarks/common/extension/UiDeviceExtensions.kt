package net.thunderbird.benchmarks.common.extension

import android.content.Intent
import android.os.SystemClock
import android.widget.EditText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import net.thunderbird.benchmarks.common.MESSAGE_LIST_RESOURCE_ID
import net.thunderbird.benchmarks.common.account.InstrumentationBenchmarkAccountProvider
import net.thunderbird.benchmarks.common.account.setup.AccountSetupRunner
import net.thunderbird.benchmarks.common.account.setup.step.AutoDiscoveryPasswordStep
import net.thunderbird.benchmarks.common.account.setup.step.AutoDiscoveryResultStep
import net.thunderbird.benchmarks.common.account.setup.step.DisplayOptionsStep
import net.thunderbird.benchmarks.common.account.setup.step.EmailAddressStep
import net.thunderbird.benchmarks.common.account.setup.step.IncomingServerSettingsStep
import net.thunderbird.benchmarks.common.account.setup.step.OutgoingServerSettingsStep
import net.thunderbird.benchmarks.common.account.setup.step.PermissionsStep
import net.thunderbird.benchmarks.common.account.setup.step.SyncOptionsStep
import net.thunderbird.benchmarks.common.account.setup.step.ThundermailStep
import net.thunderbird.benchmarks.common.account.setup.step.WelcomeStep

internal const val TIMEOUT_MS = 15_000L
private const val POLL_INTERVAL_MS = 250L

/**
 * Makes sure the app under test has an account configured, so startup lands on the message list instead of onboarding.
 *
 * The account comes from [InstrumentationBenchmarkAccountProvider].
 *
 * This is idempotent: if an account is already configured, it only launches the app once and returns. It needs to run
 * before every Gradle run, because the connected test task uninstalls the app under test afterwards.
 *
 * If the server validation reports a certificate error (e.g. a self-signed certificate), the certificate is accepted.
 *
 * The Compose screens expose their test tags as resource ids. The steps without a test tag rely on the `EditText`
 * order on screen and the prefilled email address.
 */
internal fun UiDevice.ensureAccountConfigured(packageName: String) {
    AccountSetupRunner(
        packageName = packageName,
        accountProvider = InstrumentationBenchmarkAccountProvider(),
        steps = listOf(
            WelcomeStep(),
            ThundermailStep(),
            EmailAddressStep(),
            AutoDiscoveryPasswordStep(),
            AutoDiscoveryResultStep(),
            IncomingServerSettingsStep(),
            OutgoingServerSettingsStep(),
            DisplayOptionsStep(),
            SyncOptionsStep(),
            PermissionsStep(packageName),
        ),
    ).run(this)
}

internal fun UiDevice.launch(packageName: String) {
    val context = InstrumentationRegistry.getInstrumentation().context
    val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) {
        "No launch intent found for $packageName"
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    context.startActivity(intent)
    wait(Until.hasObject(By.pkg(packageName).depth(0)), TIMEOUT_MS)
}

/**
 * Waits until the message list shows at least one item.
 *
 * The app reports fully drawn when the message list gets its first items. Waiting for them makes sure that moment is
 * part of the measurement, so the time to full display is captured.
 */
internal fun UiDevice.waitForMessageListItems(packageName: String, timeoutMs: Long) {
    pollUntil(timeoutMs, { "Message list has no items. The benchmark account needs messages in its inbox." }) {
        findObject(By.res(packageName, MESSAGE_LIST_RESOURCE_ID))?.takeIf { it.childCount > 0 }
    }
}

internal fun UiDevice.waitFor(selector: BySelector): UiObject2 {
    return checkNotNull(wait(Until.findObject(selector), TIMEOUT_MS)) { "Timed out waiting for $selector" }
}

internal fun UiDevice.waitForAny(timeoutMs: Long, vararg selectors: BySelector): BySelector {
    return pollUntil(timeoutMs, { "Timed out waiting for any of ${selectors.toList()}" }) {
        selectors.firstOrNull { hasObject(it) }
    }
}

/**
 * Polls [block] until it returns a non-null value. A [StaleObjectException], thrown when the screen changes while
 * reading it, is treated like "not there yet".
 */
internal fun <T : Any> UiDevice.pollUntil(timeoutMs: Long, lazyMessage: () -> String, block: () -> T?): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        try {
            block()?.let { return it }
        } catch (_: StaleObjectException) {
            // The screen changed while reading it, try again.
        }
        SystemClock.sleep(POLL_INTERVAL_MS)
    }
    error(lazyMessage())
}

internal fun UiDevice.findEditTexts(): List<UiObject2> {
    return findObjects(By.clazz(EditText::class.java)).sortedBy { it.visibleBounds.top }
}

internal fun UiDevice.waitForEditTexts(count: Int): List<UiObject2> {
    return pollUntil(TIMEOUT_MS, { "Timed out waiting for $count EditText(s)" }) {
        findEditTexts().takeIf { it.size == count }
    }
}
