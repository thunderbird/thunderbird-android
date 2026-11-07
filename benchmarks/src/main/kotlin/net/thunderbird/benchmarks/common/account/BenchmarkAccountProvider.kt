package net.thunderbird.benchmarks.common.account

/**
 * Gives the [BenchmarkAccount] to add to the app under test.
 */
internal fun interface BenchmarkAccountProvider {
    fun provide(): BenchmarkAccount
}
