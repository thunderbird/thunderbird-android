package net.thunderbird.feature.mail.message

import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.thunderbird.core.architecture.model.BaseUuidIdentifier
import net.thunderbird.core.architecture.model.UuidIdentifierSerializer

/**
 * Identifies an account-scoped conversation across mail folders.
 */
@Serializable(with = ThreadIdSerializer::class)
class ThreadId(
    value: Uuid,
) : BaseUuidIdentifier(value)

object ThreadIdSerializer : KSerializer<ThreadId> by UuidIdentifierSerializer(ThreadIdFactory::of)
