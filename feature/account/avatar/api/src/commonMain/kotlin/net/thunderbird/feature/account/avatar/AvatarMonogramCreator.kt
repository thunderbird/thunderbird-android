package net.thunderbird.feature.account.avatar

/**
 * Interface for creating a monogram based on a name or email address.
 *
 * This interface is used to generate a monogram, which is typically the initials of a person's name,
 * or a representation based on an email address. Implementations should handle null or empty inputs gracefully.
 */
fun interface AvatarMonogramCreator {
    /**
     * Creates a monogram for the given name or email.
     *
     * @param name The name to generate a monogram for.
     * @param email The email address to generate a monogram for.
     * @return A string representing the monogram, or an empty string if the name or email is null or empty.
     */
    fun create(name: String?, email: String?): String
}
