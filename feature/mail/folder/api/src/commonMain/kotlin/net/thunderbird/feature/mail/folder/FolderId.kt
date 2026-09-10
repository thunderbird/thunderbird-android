package net.thunderbird.feature.mail.folder

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Identifies a local folder record across all accounts.
 */
@OptIn(ExperimentalUuidApi::class)
class FolderId(
    value: Uuid,
) : BaseUuidIdentifier(value)
