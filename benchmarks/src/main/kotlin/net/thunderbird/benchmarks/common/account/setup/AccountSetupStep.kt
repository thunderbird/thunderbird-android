package net.thunderbird.benchmarks.common.account.setup

import androidx.test.uiautomator.UiDevice
import net.thunderbird.benchmarks.common.account.BenchmarkAccount

/**
 * One part of the account setup, usually one screen.
 *
 * [AccountSetupRunner] runs the steps in order. To support a new screen, add a step.
 */
internal interface AccountSetupStep {

    /**
     * Whether the step runs for [account]. If not, it is skipped.
     */
    fun isApplicable(account: BenchmarkAccount): Boolean = true

    fun execute(device: UiDevice, account: BenchmarkAccount)
}
