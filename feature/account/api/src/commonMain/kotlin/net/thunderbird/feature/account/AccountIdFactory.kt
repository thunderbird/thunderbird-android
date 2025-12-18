package net.thunderbird.feature.account

import net.thunderbird.core.architecture.model.BaseIdFactory

/**
 * Factory object for creating unique identifiers for [Account] instances.
 */
object AccountIdFactory : BaseIdFactory<AccountIdentifiable>()
