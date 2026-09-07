package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Identifies an account-scoped conversation across mail folders.
 */
@OptIn(ExperimentalUuidApi::class)
class ThreadId(
    value: Uuid,
) : BaseUuidIdentifier(value)
