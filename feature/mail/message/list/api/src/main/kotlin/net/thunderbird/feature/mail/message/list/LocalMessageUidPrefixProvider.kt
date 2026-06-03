package net.thunderbird.feature.mail.message.list

/**
 * Provides a unique prefix string for generating local message UIDs.
 *
 * Implementations should guarantee that the returned prefix is consistent and unique
 * within the context where it will be used, typically per account or per application instance.
 */
interface LocalMessageUidPrefixProvider {
    /**
     * Returns the unique prefix string used for generating local message UIDs.
     *
     * @return The prefix string to be used when creating unique identifiers for locally stored messages.
     */
    fun get(): String
}
