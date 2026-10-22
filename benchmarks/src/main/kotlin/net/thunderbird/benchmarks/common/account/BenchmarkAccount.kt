package net.thunderbird.benchmarks.common.account

/**
 * The account to add to the app under test.
 *
 * [servers] is `null` when auto discovery should find the server settings.
 */
internal class BenchmarkAccount(
    val email: String,
    val password: String,
    val displayName: String,
    val servers: BenchmarkServers?,
)

internal class BenchmarkServers(
    val incoming: String,
    val outgoing: String,
)
