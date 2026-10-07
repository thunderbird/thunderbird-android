package net.thunderbird.feature.account

import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.thunderbird.core.architecture.model.BaseUuidIdentifier
import net.thunderbird.core.architecture.model.UuidIdentifierSerializer

/**
 * Represents a unique identifier for an [Account].
 */
@Serializable(with = AccountIdSerializer::class)
class AccountId(
    value: Uuid,
) : BaseUuidIdentifier(value)

object AccountIdSerializer : KSerializer<AccountId> by UuidIdentifierSerializer(AccountIdFactory::of)
