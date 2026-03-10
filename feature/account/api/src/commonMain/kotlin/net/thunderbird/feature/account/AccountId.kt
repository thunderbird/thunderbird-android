package net.thunderbird.feature.account

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.BaseUuidIdentifier

/**
 * Represents a unique identifier for an [Account].
 */
@OptIn(ExperimentalUuidApi::class)
class AccountId(
    value: Uuid,
) : BaseUuidIdentifier(value)
