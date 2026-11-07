package net.thunderbird.benchmarks.common.account

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

private const val DEMO_EMAIL_ADDRESS = "benchmark@example.com"
private const val DEMO_PASSWORD = "demo"
private const val DEMO_DISPLAY_NAME = "Benchmark"

/**
 * Reads the account from the instrumentation arguments. Missing values fall back to a demo account.
 *
 * The values come from `local.properties`:
 * ```
 * tfa.benchmark.account.email=...
 * tfa.benchmark.account.password=...
 * tfa.benchmark.account.display_name=...
 * tfa.benchmark.account.incoming_server=...
 * tfa.benchmark.account.outgoing_server=...
 * ```
 * The account must use password authentication. OAuth is not supported.
 *
 * Set both servers, or none. Without servers, auto discovery has to find them. With servers,
 * only the host names are changed, so the other settings must work with them.
 *
 * The demo account only works in the `daily` and `debug` builds. It needs no network or real
 * credentials.
 *
 * **IMPORTANT:** The password is visible in the `am instrument` command line and in Gradle logs.
 * Use a throwaway account.
 */
internal class InstrumentationBenchmarkAccountProvider(
    private val arguments: () -> Bundle = { InstrumentationRegistry.getArguments() },
) : BenchmarkAccountProvider {

    override fun provide(): BenchmarkAccount {
        val arguments = arguments()
        return BenchmarkAccount(
            email = arguments.getString("accountEmail") ?: DEMO_EMAIL_ADDRESS,
            // The demo backend accepts any password.
            password = arguments.getString("accountPassword") ?: DEMO_PASSWORD,
            displayName = arguments.getString("accountDisplayName") ?: DEMO_DISPLAY_NAME,
            servers = arguments.toServers(),
        )
    }

    private fun Bundle.toServers(): BenchmarkServers? {
        val incomingServer = getString("accountIncomingServer")
        val outgoingServer = getString("accountOutgoingServer")
        if (incomingServer == null && outgoingServer == null) return null

        return BenchmarkServers(
            incoming = requireNotNull(incomingServer) { "tfa.benchmark.account.incoming_server is missing" },
            outgoing = requireNotNull(outgoingServer) { "tfa.benchmark.account.outgoing_server is missing" },
        )
    }
}
