package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import net.thunderbird.core.common.mail.EmailAddressParserError
import net.thunderbird.core.common.mail.EmailAddressParserException
import net.thunderbird.core.common.mail.toEmailAddressOrNull
import net.thunderbird.core.common.mail.toUserEmailAddress
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

/**
 * Validate an email address that the user wants to add to an account.
 *
 * This only allows a subset of all valid email addresses. We currently don't support international email addresses
 * and don't allow quoted local parts, or email addresses exceeding length restrictions.
 *
 * Note: Do NOT use this to validate recipients in incoming or outgoing messages. Use [String.toEmailAddressOrNull]
 * instead.
 */
class ValidateEmailAddress : UseCase.ValidateEmailAddress {

    override fun execute(emailAddress: String): ValidationOutcome {
        if (emailAddress.isBlank()) {
            return Outcome.Failure(ValidateEmailAddressError.EmptyEmailAddress)
        }

        return try {
            val parsedEmailAddress = emailAddress.toUserEmailAddress()

            if (parsedEmailAddress.warnings.isEmpty()) {
                ValidationSuccess
            } else {
                Outcome.Failure(ValidateEmailAddressError.NotAllowed)
            }
        } catch (e: EmailAddressParserException) {
            Log.v(e, "Error parsing email address: %s", emailAddress)

            val validationError = when (e.error) {
                EmailAddressParserError.AddressLiteralsNotSupported,
                EmailAddressParserError.LocalPartLengthExceeded,
                EmailAddressParserError.DnsLabelLengthExceeded,
                EmailAddressParserError.DomainLengthExceeded,
                EmailAddressParserError.TotalLengthExceeded,
                EmailAddressParserError.QuotedStringInLocalPart,
                EmailAddressParserError.LocalPartRequiresQuotedString,
                EmailAddressParserError.EmptyLocalPart,
                -> {
                    ValidateEmailAddressError.NotAllowed
                }

                else -> {
                    if ('@' in emailAddress) {
                        // We currently don't support or recognize international email addresses. So if the string
                        // contains an "@" character, we assume it's a valid email address that we don't support.
                        ValidateEmailAddressError.InvalidOrNotSupported
                    } else {
                        ValidateEmailAddressError.InvalidEmailAddress
                    }
                }
            }

            Outcome.Failure(validationError)
        }
    }

    sealed interface ValidateEmailAddressError : ValidationError {
        data object EmptyEmailAddress : ValidateEmailAddressError
        data object NotAllowed : ValidateEmailAddressError
        data object InvalidOrNotSupported : ValidateEmailAddressError
        data object InvalidEmailAddress : ValidateEmailAddressError
    }
}
