package net.thunderbird.core.featureflag.data.configstore.mapper

import kotlin.uuid.Uuid
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigKeys
import net.thunderbird.core.featureflag.model.FlagOverrides

internal class FeatureFlagConfigDefinitionMapper(private val json: Json) : ConfigMapper<FeatureFlagConfigData> {
    override fun toConfig(obj: FeatureFlagConfigData): Config = Config().apply {
        if (obj.targetingKey != null) {
            this[FeatureFlagConfigKeys.TARGETING_KEY] = obj.targetingKey.toString()
        }

        // Written even when empty: the backend only overwrites the keys it is handed, so omitting
        // this would leave the previously persisted overrides in place.
        this[FeatureFlagConfigKeys.OVERRIDES] = json.encodeToString(obj.overrides)
    }

    override fun fromConfig(config: Config): FeatureFlagConfigData = FeatureFlagConfigData(
        targetingKey = config.decodeTargetingKey(),
        overrides = config.decodeOverrides(),
    )

    private fun Config.decodeTargetingKey(): Uuid? = this[FeatureFlagConfigKeys.TARGETING_KEY]?.let(Uuid::parse)

    /** Overrides are debug-only state, so unreadable data is dropped instead of failing the store. */
    private fun Config.decodeOverrides(): FlagOverrides =
        this[FeatureFlagConfigKeys.OVERRIDES]?.let { rawJson ->
            try {
                json.decodeFromString<FlagOverrides>(rawJson)
            } catch (_: SerializationException) {
                emptyMap()
            }
        }.orEmpty()
}
