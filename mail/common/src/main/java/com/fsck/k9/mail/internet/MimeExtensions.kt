package com.fsck.k9.mail.internet

// RFC 5322, section 2.1.1
internal const val RECOMMENDED_MAX_LINE_LENGTH = 78

// RFC 2045: tspecials :=  "(" / ")" / "<" / ">" / "@" / "," / ";" / ":" / "\" / <"> / "/" / "[" / "]" / "?" / "="
private val TSPECIALS = charArrayOf('(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=')

private val ATEXT_SPECIAL = charArrayOf(
    '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~',
)

// RFC 5234: HTAB = %x09
internal const val HTAB = '\t'

// RFC 5234: SP = %x20
internal const val SPACE = ' '

// RFC 5234: CRLF = %d13.10
internal const val CRLF = "\r\n"

internal const val CR = '\r'
internal const val LF = '\n'
internal const val DQUOTE = '"'
internal const val SEMICOLON = ';'
internal const val EQUALS_SIGN = '='
internal const val ASTERISK = '*'
internal const val SINGLE_QUOTE = '\''
internal const val BACKSLASH = '\\'

internal fun Char.isTSpecial() = this in TSPECIALS

// RFC 2045: token := 1*<any (US-ASCII) CHAR except SPACE, CTLs, or tspecials>
// RFC 5234: CTL = %x00-1F / %x7F
internal fun Char.isTokenChar() = isVChar() && !isTSpecial()

// RFC 5234: VCHAR = %x21-7E
internal fun Char.isVChar() = code in 33..126

// RFC 5234: WSP =  SP / HTAB
internal fun Char.isWsp() = this == SPACE || this == HTAB

internal fun Char.isWspOrCrlf() = this == SPACE || this == HTAB || this == CR || this == LF

// RFC 2231: attribute-char := <any (US-ASCII) CHAR except SPACE, CTLs, "*", "'", "%", or tspecials>
internal fun Char.isAttributeChar() = isVChar() && this != '*' && this != '\'' && this != '%' && !isTSpecial()

// RFC 5322: ctext = %d33-39 / %d42-91 / %d93-126
internal fun Char.isCText() = code.let { it in 33..39 || it in 42..91 || it in 93..126 }

// RFC 5234: DIGIT = %x30-39 ; 0-9
internal fun Char.isDIGIT() = this in '0'..'9'

// RFC 5234: ALPHA = %x41-5A / %x61-7A ; A-Z / a-z
internal fun Char.isALPHA() = this in 'A'..'Z' || this in 'a'..'z'

// RFC 5322: atext = ALPHA / DIGIT / "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "/" / "=" / "?" / "^" /
//                   "_" / "`" / "{" / "|" / "}" / "~"
internal fun Char.isAText() = isALPHA() || isDIGIT() || this in ATEXT_SPECIAL

// RFC 5322: Printable US-ASCII characters not including "[", "]", or "\"
// dtext = %d33-90 / %d94-126 / obs-dtext
internal fun Char.isDText() = code.let { it in 33..90 || it in 94..126 }
