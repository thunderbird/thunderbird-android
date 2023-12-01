package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.mail.EmailAddressParserError
import app.k9mail.core.common.mail.EmailAddressParserException
import app.k9mail.core.common.mail.toEmailAddressOrNull
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import com.fsck.k9.logging.Timber

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

    override fun execute(emailAddress: String): ValidationResult {
        if (emailAddress.isBlank()) {
            return ValidationResult.Failure(ValidateEmailAddressError.EmptyEmailAddress)
        }

        return try {
            val parsedEmailAddress = emailAddress.toUserEmailAddress()

            if (parsedEmailAddress.warnings.isEmpty()) {
                ValidationResult.Success
            } else {
                ValidationResult.Failure(ValidateEmailAddressError.NotAllowed)
            }
        } catch (e: EmailAddressParserException) {
            Timber.v(e, "Error parsing email address: %s", emailAddress)

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

            ValidationResult.Failure(validationError)
        }
    }

    sealed interface ValidateEmailAddressError : ValidationError {
        data object EmptyEmailAddress : ValidateEmailAddressError
        data object NotAllowed : ValidateEmailAddressError
        data object InvalidOrNotSupported : ValidateEmailAddressError
        data object InvalidEmailAddress : ValidateEmailAddressError
    }
}
