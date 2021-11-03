package com.fsck.k9.mail.internet;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DecoderUtilTest {
    private static final String INVALID = "=?utf-8?Q??=";


    @Test
    public void decodeEncodedWords_withInvalidEncodedWord_shouldReturnInputText() {
        // We use INVALID as instance of an invalid encoded word in tests. If at some point we decide to change the code
        // to recognize empty encoded text as valid and decode it to an empty string, a lot of tests will break.
        // Hopefully this test will help the developer figure out why the other tests broke.
        assertInputDecodesToExpected(INVALID, INVALID);
    }

    @Test
    public void decodeEncodedWords_with_unencoded_data_returns_original_text() {
        assertInputDecodesToExpected("abc", "abc");
    }

    @Test
    public void decodeEncodedWords_withAsciiCharset_encoded_data_returns_text() {
        assertInputDecodesToExpected("=?us-ascii?q?abc?=", "abc");
    }

    @Test
    public void decodeEncodedWords_withStartOnly_encoding_format_returnAsText() {
        assertInputDecodesToExpected("=?", "=?");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordAndOnlyStartOfEncodedWord_shouldDecodeAndAddSuffix() {
        assertInputDecodesToExpected("=?utf-8?Q?abc?= =?", "abc =?");
    }

    @Test
    public void decodeEncodedWords_withStartAndSeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=??", "=??");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordAndOnlyStartAndSeparatorOfEncodedWord_shouldDecodeAndAddSuffix() {
        assertInputDecodesToExpected("=?utf-8?Q?abc?= =??", "abc =??");
    }

    @Test
    public void decodeEncodedWords_withStartAnd2SeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=???", "=???");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordAndOnlyStartAndTwoSeparatorsOfEncodedWord_shouldDecodeAndAddSuffix() {
        assertInputDecodesToExpected("=?utf-8?Q?abc?= =???", "abc =???");
    }

    @Test
    public void decodeEncodedWords_withStartAnd3SeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=????", "=????");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordAndOnlyStartAndThreeSeparatorsOfEncodedWord_shouldDecodeAndAddSuffix() {
        assertInputDecodesToExpected("=?utf-8?Q?abc?= =????", "abc =????");
    }

    @Test
    public void decodeEncodedWords_withSeparatorsOnly_returnAsText() {
        assertInputDecodesToExpected("=????=", "=????=");
    }

    @Test
    public void decodeEncodedWords_withMissingCharset_returnAsText() {
        assertInputDecodesToExpected("=??q??=", "=??q??=");
    }

    @Test
    public void decodeEncodedWords_withTextAndMissingCharset_returnAsText() {
        assertInputDecodesToExpected("=??q?a?=", "a");
    }

    @Test
    public void decodeEncodedWords_withNoTextCharsetOrEncoding_returnAsText() {
        assertInputDecodesToExpected("=??=", "=??=");
    }

    @Test
    public void decodeEncodedWords_with_MissingEncodingAndData_returnAsText() {
        assertInputDecodesToExpected("=?x?=", "=?x?=");
    }

    @Test
    public void decodeEncodedWords_withMissingEncoding_returnAsText() {
        assertInputDecodesToExpected("=?x??=", "=?x??=");
    }

    @Test
    public void decodeEncodedWords_with_incompleteEncodingFormat_returnAsText() {
        assertInputDecodesToExpected("=?x?q?=", "=?x?q?=");
    }

    @Test
    public void decodeEncodedWords_with_unrecognisedEncoding_withEmptyEncodedData_returnAsText() {
        assertInputDecodesToExpected("=?x?q??=", "=?x?q??=");
    }

    @Test
    public void decodeEncodedWords_withUnrecognisedEncoding_withEncodedData_return_encoded_data() {
        assertInputDecodesToExpected("=?x?q?X?=", "X");
    }

    @Test
    public void decodeEncodedWords_withInvalidBase64String_returnsEmptyString() {
        assertInputDecodesToExpected("=?us-ascii?b?ab#?=", "");
    }

    @Test
    public void decodeEncodedWords_withPartiallyEncoded_returnsBothSections() {
        assertInputDecodesToExpected("=?us-ascii?q?abc?= =?", "abc =?");
    }

    @Test
    public void decodeEncodedWords_withPartiallyEncodedAfter_returnsBothSections() {
        assertInputDecodesToExpected("def=?us-ascii?q?abc?=", "defabc");
    }

    @Test
    public void decodeEncodedWords_withUnrecognisedCharset_returnsEncodedData() {
        assertInputDecodesToExpected("=?x?= =?", "=?x?= =?");
    }

    @Test
    public void decodeEncodedWords_withMultipleEncodedSections_decodesBoth() {
        assertInputDecodesToExpected("=?us-ascii?q?abc?= =?us-ascii?q?def?=", "abcdef");
    }

    @Test
    public void decodeEncodedWords_withMultipleEncodedSections_decodesSequentialSectionTogether() {
        // Splitting mid-character is RFC2047 non-compliant but seen in practice.
        // "=?utf-8?B?b2hhaSDw?=" individually decodes to "ohai �"
        // "=?utf-8?B?n5Kp==?=" individually decodes to "���"
        // (invalid bytes in a UTF-8 sequence are replaced with the replacement character)
        assertInputDecodesToExpected("=?utf-8?B?b2hhaSDw?= =?utf-8?B?n5Kp?=", "ohai 💩");
    }

    @Test
    public void decodeEncodedWords_withMultipleEncodedSectionsButCharsetAndEncodingDifferingInCase_decodesSequentialSectionTogether() {
        assertInputDecodesToExpected("=?utf-8?B?b2hhaSDw?= =?UTF-8?b?n5Kp?=", "ohai 💩");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordWhitespaceInvalidEncodedWord_shouldOnlyDecodeEncodedWord() {
        assertInputDecodesToExpected("=?utf-8?Q?abc?=   " + INVALID, "abc   " + INVALID);
    }

    @Test
    public void decodeEncodedWords_withInvalidEncodedWordWhitespaceInvalidEncodedWord_shouldReturnInputText() {
        String input = INVALID + "   " + INVALID;
        assertInputDecodesToExpected(input, input);
    }

    @Test
    public void decodeEncodedWords_withEncodedWordNonWhitespaceSeparatorEncodedWord_shouldDecodeBothAndKeepSeparator() {
        assertInputDecodesToExpected("=?utf-8?Q?ab?= -- =?utf-8?Q?cd?=", "ab -- cd");
    }

    @Test
    public void decodeEncodedWords_withInvalidEncodedWordWhitespaceEncodedWord_shouldOnlyDecodeEncodedWord() {
        assertInputDecodesToExpected(INVALID + "   =?utf-8?Q?abc?=", INVALID + "   abc");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordFollowedByEncodedWordWithDifferentEncoding_shouldDecodeIndividually() {
        assertInputDecodesToExpected("=?utf-8?Q?ab?= =?utf-8?B?Y2Q=?=", "abcd");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordSeparatorEncodedWordWithDifferentEncoding_shouldDecodeIndividuallyAndKeepSeparator() {
        assertInputDecodesToExpected("=?utf-8?Q?ab?= / =?utf-8?B?Y2Q=?=", "ab / cd");
    }

    @Test
    public void decodeEncodedWords_withEncodedWordFollowedByEncodedWordWithDifferentCharset_shouldDecodeIndividually() {
        assertInputDecodesToExpected("=?us-ascii?Q?oh_no_?= =?utf-8?Q?=F0=9F=92=A9?=", "oh no 💩");
    }

    @Test
    public void decodeEncodedWords_withTwoCompleteEncodedWords_shouldProvideBoth() {
        assertInputDecodesToExpected("=?UTF-8?B?W+aWsOioguWWrl0g6aGn5a6iOiB4eHhAeHh4LmNvbSDmnInmlrDoqILllq46ICMyMDE4MA==?= " +
                "=?UTF-8?B?MTE4MTIzNDU2Nzg=?=", "[新訂單] 顧客: xxx@xxx.com 有新訂單: #2018011812345678");
    }

    @Test
    public void decodeEncodedWords_withRFC2047examples_decodesCorrectly() {
        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?=)", "(a)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?= b)", "(a b)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?= =?ISO-8859-1?Q?b?=)", "(ab)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?=  =?ISO-8859-1?Q?b?=)", "(ab)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?=     \n    =?ISO-8859-1?Q?b?=)", "(ab)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a_b?=)", "(a b)");

        assertInputDecodesToExpected("(=?ISO-8859-1?Q?a?= =?ISO-8859-2?Q?_b?=)", "(a b)");
    }

    @Test
    public void decodeEncodedWords_withLanguageInformation() {
        // Example from RFC 2231, section 5. This is unlikely to ever fail because our charset fallback is US-ASCII.
        assertInputDecodesToExpected("=?US-ASCII*EN?Q?Keith_Moore?= <moore@cs.utk.edu>",
                "Keith Moore <moore@cs.utk.edu>");

        assertInputDecodesToExpected("=?utf-8*de?b?R3LDvMOfZQ==?=", "Grüße");
    }

    @Test
    public void decodeEncodedWords_withMultipleIso2022JpEncodedWordsProperlyEndingWithSwitchingToAscii() {
        // If we try to combine the base64-decoded data of both encoded words and only then perform the charset
        // decoding, we end up with an escape sequence switching to ASCII (end of first encoded word) followed by an
        // escape sequence switching to JIS X 0208:1983 (start of second encoded word). The decoder on Android reports
        // an error for this case, leading to a replacement character being inserted.
        // We use the ISO-2022-JP-TEST charset to get Android's behavior on the JVM. See TestCharsetProvider.
        assertInputDecodesToExpected("=?ISO-2022-JP-TEST?B?GyRCRnxLXDhsJEhGfEtcOGwkSEZ8S1w4bCROJUElJyVDGyhC?=\r\n" +
                " =?ISO-2022-JP-TEST?B?GyRCJS8bKEI=?=", "日本語と日本語と日本語のチェック");
    }

    private void assertInputDecodesToExpected(String input, String expected) {
        String decodedText = DecoderUtil.decodeEncodedWords(input, null);
        assertEquals(expected, decodedText);
    }
}
