package com.fsck.k9

import java.util.regex.Pattern

class EmailAddressValidator {

    fun isValidAddressOnly(text: CharSequence): Boolean = EMAIL_ADDRESS_PATTERN.matcher(text).matches()

    companion object {

        // https://www.rfc-editor.org/rfc/rfc2396.txt (3.2.2)
        // https://www.rfc-editor.org/rfc/rfc5321.txt (4.1.2)

        private const val ALPHA = "[a-zA-Z]"
        private const val ALPHANUM = "[a-zA-Z0-9]"
        private const val ATEXT = "[0-9a-zA-Z!#$%&'*+\\-/=?^_`{|}~]"
        private const val QCONTENT = "([\\p{Graph}\\p{Blank}&&[^\"\\\\]]|\\\\[\\p{Graph}\\p{Blank}])"
        private const val TOP_LABEL = "(($ALPHA($ALPHANUM|\\-|_)*$ALPHANUM)|$ALPHA)"
        private const val DOMAIN_LABEL = "(($ALPHANUM($ALPHANUM|\\-|_)*$ALPHANUM)|$ALPHANUM)"
        private const val HOST_NAME = "((($DOMAIN_LABEL\\.)+$TOP_LABEL)|$DOMAIN_LABEL)"

        private val EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "^($ATEXT+(\\.$ATEXT+)*|\"$QCONTENT+\")" +
                "\\@$HOST_NAME",
        )
    }
}
