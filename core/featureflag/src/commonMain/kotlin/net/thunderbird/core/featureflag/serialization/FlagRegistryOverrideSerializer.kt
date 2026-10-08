package net.thunderbird.core.featureflag.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.FlagRegistryOverride

class FlagRegistryOverrideSerializer(
    private val k9Factory: AppVariantOverrides.Factory,
    private val thunderbirdFactory: AppVariantOverrides.Factory,
) : KSerializer<FlagRegistryOverride> {
    private val delegate = FlagRegistryOverrideSurrogate.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName = requireNotNull(FlagRegistryOverride::class.qualifiedName),
        original = delegate.descriptor,
    )

    override fun serialize(encoder: Encoder, value: FlagRegistryOverride) {
        encoder.encodeSerializableValue(
            serializer = delegate,
            value = FlagRegistryOverrideSurrogate(k9 = value.k9, thunderbird = value.thunderbird),
        )
    }

    override fun deserialize(decoder: Decoder): FlagRegistryOverride {
        val surrogate = decoder.decodeSerializableValue(deserializer = delegate)
        return FlagRegistryOverride(
            k9 = k9Factory.create(surrogate.k9),
            thunderbird = thunderbirdFactory.create(surrogate.thunderbird),
        )
    }
}

@Serializable
private data class FlagRegistryOverrideSurrogate(
    val k9: AppVariantOverridesRawType,
    val thunderbird: AppVariantOverridesRawType,
)
