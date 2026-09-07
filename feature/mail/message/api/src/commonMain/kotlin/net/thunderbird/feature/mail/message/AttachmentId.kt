package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Identifies a persisted attachment record across all accounts.
 */
@OptIn(ExperimentalUuidApi::class)
class AttachmentId(
    value: Uuid,
) : BaseUuidIdentifier(value)
