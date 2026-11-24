package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory

/**
 * Factory for creating [MessageId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object LegacyMessageIdFactory : LegacyEntityIdFactory<MessageId>(
    entityByteRepresentation = ByteRepresentation.MESSAGE,
    fromUuid = ::MessageId,
)
