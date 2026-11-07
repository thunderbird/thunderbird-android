package net.thunderbird.feature.mail.message

import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.thunderbird.core.architecture.model.BaseUuidIdentifier
import net.thunderbird.core.architecture.model.UuidIdentifierSerializer

/**
 * Identifies a local message record across all accounts.
 */
@Serializable(with = MessageIdSerializer::class)
public class MessageId(
    value: Uuid,
) : BaseUuidIdentifier(value)

public object MessageIdSerializer : KSerializer<MessageId> by UuidIdentifierSerializer(MessageIdFactory::of)
