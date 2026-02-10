package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local

private const val TAG = "ContributionConfigMapper"

internal class ContributionConfigMapper(
    private val logger: Logger,
) : Local.ContributionConfigMapper {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun toConfig(obj: ContributionConfig): Config = Config().apply {
        this[ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION] = json.encodeToString(obj.lastPurchasedContribution)
    }

    override fun fromConfig(config: Config): ContributionConfig? {
        val jsonString = config[ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION] ?: return ContributionConfig.DEFAULT
        return try {
            val purchase = json.decodeFromString<ContributionPurchase>(jsonString)
            ContributionConfig(lastPurchasedContribution = purchase)
        } catch (e: SerializationException) {
            logger.error(tag = TAG, throwable = e) {
                "Failed to deserialize ContributionConfig from config: ${e.message}"
            }
            ContributionConfig.DEFAULT
        } catch (e: IllegalArgumentException) {
            logger.error(tag = TAG, throwable = e) {
                "Failed to deserialize ContributionConfig from config: ${e.message}"
            }
            ContributionConfig.DEFAULT
        }
    }
}
