package net.thunderbird.core.common.mail

/**
 * Configuration to control the behavior when parsing an email address into [EmailAddress].
 *
 * @param isLocalPartLengthCheckEnabled When this is `true` the length of the local part is checked to make sure it
 * doesn't exceed the specified limit (see RFC 5321, 4.5.3.1.1.).
 *
 * @param isEmailAddressLengthCheckEnabled When this is `true` the length of the whole email address is checked to make
 * sure it doesn't exceed the specified limit (see RFC 5321, 4.5.3.1.3.; The maximum length of 'Path' indirectly limits
 * the length of 'Mailbox').
 *
 * @param isQuotedLocalPartAllowed When this is `true`, the parsing step allows email addresses with a local part
 * encoded as quoted string, e.g. `"foo bar"@domain.example`. Otherwise, the parser will throw an
 * [EmailAddressParserException] as soon as a quoted string is encountered.
 * Quoted strings in local parts are not widely used. It's recommended to disallow them whenever possible.
 *
 * @param isLocalPartRequiringQuotedStringAllowed Email addresses whose local part requires the use of a quoted string
 * are only allowed when this is `true`. This is separate from [isQuotedLocalPartAllowed] because one might want to
 * allow email addresses that unnecessarily use a quoted string, e.g. `"test"@domain.example`
 * ([isQuotedLocalPartAllowed] = `true`, [isLocalPartRequiringQuotedStringAllowed] = `false`; [EmailAddress] will not
 * retain the original form and treat this address exactly like `test@domain.example`). When allowing this, remember to
 * use the value of [EmailAddress.address] instead of retaining the original user input.
 *
 * The value of this property is ignored if [isQuotedLocalPartAllowed] is `false`.
 *
 * @param isEmptyLocalPartAllowed Email addresses with an empty local part (e.g. `""@domain.example`) are only allowed
 * if this value is `true`.
 *
 * The value of this property is ignored if at least one of [isQuotedLocalPartAllowed] and
 * [isLocalPartRequiringQuotedStringAllowed] is `false`.
 */
data class EmailAddressParserConfig(
    val isLocalPartLengthCheckEnabled: Boolean,
    val isEmailAddressLengthCheckEnabled: Boolean,
    val isQuotedLocalPartAllowed: Boolean,
    val isLocalPartRequiringQuotedStringAllowed: Boolean,
    val isEmptyLocalPartAllowed: Boolean = false,
) {
    companion object {
        /**
         * This allows local parts requiring quoted strings and disables length checks for the local part and the
         * whole email address.
         */
        val RELAXED = EmailAddressParserConfig(
            isLocalPartLengthCheckEnabled = false,
            isEmailAddressLengthCheckEnabled = false,
            isQuotedLocalPartAllowed = true,
            isLocalPartRequiringQuotedStringAllowed = true,
            isEmptyLocalPartAllowed = false,
        )

        /**
         * This only allows a subset of valid email addresses. Use this when validating the email address a user wants
         * to add to an account/identity.
         */
        val LIMITED = EmailAddressParserConfig(
            isLocalPartLengthCheckEnabled = true,
            isEmailAddressLengthCheckEnabled = true,
            isQuotedLocalPartAllowed = false,
            isLocalPartRequiringQuotedStringAllowed = false,
            isEmptyLocalPartAllowed = false,
        )
    }
}
