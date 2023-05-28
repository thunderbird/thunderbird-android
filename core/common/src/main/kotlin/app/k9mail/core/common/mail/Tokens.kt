@file:Suppress("MagicNumber")

package app.k9mail.core.common.mail

internal const val DQUOTE = '"'
internal const val DOT = '.'
internal const val AT = '@'
internal const val BACKSLASH = '\\'
internal const val HYPHEN = '-'

internal val ATEXT_EXTRA = charArrayOf(
    '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~',
)

// RFC 5234: ALPHA = %x41-5A / %x61-7A    ; A-Z / a-z
internal val Char.isALPHA
    get() = this in 'A'..'Z' || this in 'a'..'z'

// RFC 5234: DIGIT = %x30-39    ; 0-9
internal val Char.isDIGIT
    get() = this in '0'..'9'

// RFC 5322:
// atext = ALPHA / DIGIT /    ; Printable US-ASCII
//         "!" / "#" /        ;  characters not including
//         "$" / "%" /        ;  specials.  Used for atoms.
//         "&" / "'" /
//         "*" / "+" /
//         "-" / "/" /
//         "=" / "?" /
//         "^" / "_" /
//         "`" / "{" /
//         "|" / "}" /
//         "~"
internal val Char.isAtext
    get() = isALPHA || isDIGIT || this in ATEXT_EXTRA

// RFC 5321: qtextSMTP = %d32-33 / %d35-91 / %d93-126
internal val Char.isQtext
    get() = code.let { it in 32..33 || it in 35..91 || it in 93..126 }

// RFC 5321: second character of quoted-pairSMTP = %d92 %d32-126
internal val Char.isQuotedChar
    get() = code in 32..126

// RFC 5321:
// Dot-string = Atom *("."  Atom)
// Atom       = 1*atext
internal val String.isDotString: Boolean
    get() {
        if (isEmpty() || this[0] == DOT || this[lastIndex] == DOT) return false
        for (i in 0..lastIndex) {
            val character = this[i]
            when {
                character == DOT -> if (this[i - 1] == DOT) return false
                character.isAtext -> Unit
                else -> return false
            }
        }

        return true
    }

// RFC 5321: Let-dig = ALPHA / DIGIT
internal val Char.isLetDig
    get() = isALPHA || isDIGIT
