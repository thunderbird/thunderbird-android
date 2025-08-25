package net.thunderbird.feature.account.avatar

/**
 * Creates a monogram based on a name or email address.
 *
 * This implementation generates a monogram by taking the first two characters of the name or email,
 * removing spaces, and converting them to uppercase.
 */
class DefaultAvatarMonogramCreator : AvatarMonogramCreator {
    override fun create(name: String?, email: String?): String {
        return if (name != null && name.isNotEmpty()) {
            composeAvatarMonogram(name)
        } else if (email != null && email.isNotEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(name: String): String {
        return name.replace(" ", "").take(2).uppercase()
    }

    private companion object {
        private const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
