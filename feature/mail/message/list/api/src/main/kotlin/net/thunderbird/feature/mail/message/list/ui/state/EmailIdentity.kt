package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.ui.graphics.Color

/**
 * Represents the visual identity of an email address.
 *
 * This is used to consistently display a user's avatar and associated color
 * throughout the UI.
 *
 * @param email The email address associated with this identity.
 * @param color A generated color associated with the email address, often used for placeholder avatars.
 * @param avatar The avatar information, which could be a URI for an image or initials.
 */
data class EmailIdentity(
    val email: String,
    val color: Color,
    val avatar: Avatar,
)
