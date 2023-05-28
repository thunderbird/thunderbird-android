package app.k9mail.core.common.mail

enum class EmailAddressParserError(internal val message: String) {
    UnexpectedEndOfInput("End of input reached unexpectedly"),
    ExpectedEndOfInput("Expected end of input"),
    InvalidLocalPart("Expected 'Dot-string' or 'Quoted-string'"),
    InvalidDotString("Expected 'Dot-string'"),
    InvalidQuotedString("Expected 'Quoted-string'"),
    InvalidDomainPart("Expected 'Domain' or 'address-literal'"),
    AddressLiteralsNotSupported("Address literals are not supported"),

    LocalPartLengthExceeded("Local part exceeds maximum length of $MAXIMUM_LOCAL_PART_LENGTH characters"),
    DnsLabelLengthExceeded("DNS labels exceeds maximum length of $MAXIMUM_DNS_LABEL_LENGTH characters"),
    DomainLengthExceeded("Domain exceeds maximum length of $MAXIMUM_DOMAIN_LENGTH characters"),
    TotalLengthExceeded("The email address exceeds the maximum length of $MAXIMUM_EMAIL_ADDRESS_LENGTH characters"),

    QuotedStringInLocalPart("Quoted string in local part is not allowed by config"),
    LocalPartRequiresQuotedString("Local part requiring the use of a quoted string is not allowed by config"),
    EmptyLocalPart("Empty local part is not allowed by config"),

    UnexpectedCharacter("Caller needs to provide message"),
}
