package app.k9mail.core.common.mail

/**
 * Configuration to control the behavior when parsing an email address into [EmailAddress].
 *
 * @param allowQuotedLocalPart When this is `true`, the parsing step allows email addresses with a local part encoded
 * as quoted string, e.g. `"foo bar"@domain.example`. Otherwise, the parser will throw an [EmailAddressParserException]
 * as soon as a quoted string is encountered.
 * Quoted strings in local parts are not widely used. It's recommended to disallow them whenever possible.
 *
 * @param allowLocalPartRequiringQuotedString Email addresses whose local part requires the use of a quoted string are
 * only allowed when this is `true`. This is separate from [allowQuotedLocalPart] because one might want to allow email
 * addresses that unnecessarily use a quoted string, e.g. `"test"@domain.example` ([allowQuotedLocalPart] = `true`,
 * [allowLocalPartRequiringQuotedString] = `false`; [EmailAddress] will not retain the original form and treat this
 * address exactly like `test@domain.example`). When allowing this, remember to use the value of [EmailAddress.address]
 * instead of retaining the original user input.
 *
 * The value of this property is ignored if [allowQuotedLocalPart] is `false`.
 *
 * @param allowEmptyLocalPart Email addresses with an empty local part (e.g. `""@domain.example`) are only allowed if
 * this value is `true`.
 *
 * The value of this property is ignored if at least one of [allowQuotedLocalPart] and
 * [allowLocalPartRequiringQuotedString] is `false`.
 */
data class EmailAddressParserConfig(
    val allowQuotedLocalPart: Boolean,
    val allowLocalPartRequiringQuotedString: Boolean,
    val allowEmptyLocalPart: Boolean = false,
) {
    companion object {
        /**
         * This configuration should match what `EmailAddressValidator` currently allows.
         */
        val DEFAULT = EmailAddressParserConfig(
            allowQuotedLocalPart = true,
            allowLocalPartRequiringQuotedString = true,
            allowEmptyLocalPart = false,
        )
    }
}
