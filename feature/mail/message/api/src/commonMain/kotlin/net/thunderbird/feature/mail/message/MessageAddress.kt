package net.thunderbird.feature.mail.message

import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * An email address with its display name.
 *
 * @property value The email address itself.
 * @property label Display name shown to the user, e.g. "Jane Doe".
 */
@PiiSafe.HasPii
data class MessageAddress(
    @get:PiiSafe.Mask
    val value: String,
    val label: String,
)
