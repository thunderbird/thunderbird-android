package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class EncoderUtilTest {
    @Test
    fun singleNonAsciiCharacter() {
        val result = EncoderUtil.encodeEncodedWord("123456789Ä")

        assertThat(result).isEqualTo("=?UTF-8?Q?123456789=C3=84?=")
    }

    @Test
    fun onlyNonAsciiCharacters() {
        val result = EncoderUtil.encodeEncodedWord("ÄÖÜÄÖÜÄÖÜÄ")

        assertThat(result).isEqualTo("=?UTF-8?B?w4TDlsOcw4TDlsOcw4TDlsOcw4Q=?=")
    }

    @Test
    fun nonAsciiCharactersThatNeedToBeEncodedAsMultipleEncodedWords() {
        val result = EncoderUtil.encodeEncodedWord(
            "Re: \uD83D\uDC15\uD83D\uDC36\uD83D\uDC29\uD83D\uDC08\uD83D\uDC31\uD83D\uDC00\uD83D\uDC01\uD83D\uDC2D" +
                "\uD83D\uDC39\uD83D\uDC22\uD83D\uDC07\uD83D\uDC30\uD83D\uDC13\uD83D\uDC14\uD83D\uDC23\uD83D\uDC24" +
                "\uD83D\uDC25\uD83D\uDC26\uD83D\uDC0F\uD83D\uDC11\uD83D\uDC10",
        )

        assertThat(result).isEqualTo(
            "=?UTF-8?B?UmU6IPCfkJXwn5C28J+QqfCfkIjwn5Cx8J+QgPCfkIHwn5Ct8J+QuQ==?= " +
                "=?UTF-8?B?8J+QovCfkIfwn5Cw8J+Qk/CfkJTwn5Cj?= " +
                "=?UTF-8?B?8J+QpPCfkKXwn5Cm8J+Qj/CfkJHwn5CQ?=",
        )
    }
}
