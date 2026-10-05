package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory

/**
 * Factory for creating [ThreadId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object LegacyThreadIdFactory : LegacyEntityIdFactory<ThreadId>(
    entityByteRepresentation = ByteRepresentation.THREADS,
    fromUuid = ::ThreadId,
)
