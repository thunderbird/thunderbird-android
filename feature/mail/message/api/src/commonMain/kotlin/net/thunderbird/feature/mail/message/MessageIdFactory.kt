package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.BaseUuidIdentifierFactory

/**
 * Factory for creating [MessageId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object MessageIdFactory : BaseUuidIdentifierFactory<MessageId>(::MessageId)
