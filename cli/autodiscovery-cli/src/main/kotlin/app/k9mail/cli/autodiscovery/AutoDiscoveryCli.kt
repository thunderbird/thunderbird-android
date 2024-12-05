package app.k9mail.cli.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.Settings
import app.k9mail.autodiscovery.autoconfig.AutoconfigUrlConfig
import app.k9mail.autodiscovery.autoconfig.createIspDbAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createMxLookupAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createProviderAutoconfigDiscovery
import app.k9mail.core.common.mail.toUserEmailAddress
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient.Builder
import org.koin.core.time.measureDurationForResult

class AutoDiscoveryCli : CliktCommand() {
    private val httpsOnly by option(help = "Only perform Autoconfig lookups using HTTPS").flag()
    private val includeEmailAddress by option(help = "Include email address in Autoconfig lookups").flag()

    private val emailAddress by argument(name = "email", help = "Email address")

    override fun help(context: Context) =
        "Performs the auto-discovery steps used by Thunderbird for Android to find mail server settings"

    override fun run() {
        echo("Attempting to find mail server settings for <$emailAddress>…")
        echo()

        val config = AutoconfigUrlConfig(
            httpsOnly = httpsOnly,
            includeEmailAddress = includeEmailAddress,
        )

        val (discoveryResult, durationInMillis) = measureDurationForResult {
            runAutoDiscovery(config)
        }

        if (discoveryResult is Settings) {
            echo("Found the following mail server settings:")
            AutoDiscoveryResultFormatter(::echo).output(discoveryResult)
        } else {
            echo("Couldn't find any mail server settings.")
        }

        echo()
        echo("Duration: ${durationInMillis.toDuration(MILLISECONDS)}")
    }

    private fun runAutoDiscovery(config: AutoconfigUrlConfig): AutoDiscoveryResult {
        val okHttpClient = Builder().build()
        try {
            val providerDiscovery = createProviderAutoconfigDiscovery(okHttpClient, config)
            val ispDbDiscovery = createIspDbAutoconfigDiscovery(okHttpClient)
            val mxDiscovery = createMxLookupAutoconfigDiscovery(okHttpClient, config)

            val runnables = listOf(providerDiscovery, ispDbDiscovery, mxDiscovery)
                .flatMap { it.initDiscovery(emailAddress.toUserEmailAddress()) }
            val serialRunner = SerialRunner(runnables)

            return runBlocking {
                serialRunner.run()
            }
        } finally {
            okHttpClient.dispatcher.executorService.shutdown()
        }
    }
}
