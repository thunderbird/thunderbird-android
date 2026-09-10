package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.BaseUuidIdentifierFactory

/**
 * Factory for creating [AttachmentId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object AttachmentIdFactory : BaseUuidIdentifierFactory<AttachmentId>(::AttachmentId)
