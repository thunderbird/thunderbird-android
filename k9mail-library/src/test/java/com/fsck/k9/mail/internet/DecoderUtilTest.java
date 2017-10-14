package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.K9LibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(K9LibRobolectricTestRunner.class)
public class DecoderUtilTest {

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
    public void decodeEncodedWords_withStartAndSeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=??", "=??");
    }

    @Test
    public void decodeEncodedWords_withStartAnd2SeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=???", "=???");
    }

    @Test
    public void decodeEncodedWords_withStartAnd3SeparatorOnly_returnAsText() {
        assertInputDecodesToExpected("=????", "=????");
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
        assertInputDecodesToExpected("=?us-ascii?b?abc?=", "");
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
        assertInputDecodesToExpected("=?utf-8?B?b2hhaSDw?= =?utf-8?B?n5Kp?=", "ohai 💩");
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


    private void assertInputDecodesToExpected(String input, String expected) {
        String decodedText = DecoderUtil.decodeEncodedWords(input, null);
        assertEquals(expected, decodedText);
    }
}
