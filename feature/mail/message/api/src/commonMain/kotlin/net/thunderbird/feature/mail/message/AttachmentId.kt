package net.thunderbird.feature.mail.message

import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.thunderbird.core.architecture.model.BaseUuidIdentifier
import net.thunderbird.core.architecture.model.UuidIdentifierSerializer

/**
 * Identifies a persisted attachment record across all accounts.
 */
@Serializable(with = AttachmentIdSerializer::class)
class AttachmentId(
    value: Uuid,
) : BaseUuidIdentifier(value)

object AttachmentIdSerializer : KSerializer<AttachmentId> by UuidIdentifierSerializer(AttachmentIdFactory::of)
