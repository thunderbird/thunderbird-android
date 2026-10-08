package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Identifies a local message record across all accounts.
 */
@OptIn(ExperimentalUuidApi::class)
class MessageId(
    value: Uuid,
) : BaseUuidIdentifier(value)
