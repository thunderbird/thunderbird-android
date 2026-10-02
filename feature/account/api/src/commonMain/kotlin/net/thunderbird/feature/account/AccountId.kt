package net.thunderbird.feature.account

import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Represents a unique identifier for an [Account].
 */
class AccountId(
    value: Uuid,
) : BaseUuidIdentifier(value)
