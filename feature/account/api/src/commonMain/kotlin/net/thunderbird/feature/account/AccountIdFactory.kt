package net.thunderbird.feature.account

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.BaseUuidIdentifierFactory

/**
 * Factory object for creating unique identifiers for [Account] instances.
 */
@OptIn(ExperimentalUuidApi::class)
object AccountIdFactory : BaseUuidIdentifierFactory<AccountId>(::AccountId)
