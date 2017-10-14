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
        //Splitting mid-character is RFC2047 non-compliant but seen in practice.
        String input = "=?utf-8?B?5Liq5Lq66YKu566xOkJVRyAjMzAyNDY6OumCruS7tuato+aWh+mZhOS7tuWQ?=\n" +
                "=?utf-8?B?jeensOecgeeVpeaYvuekuuS8mOWMlg==?=";
        assertInputDecodesToExpected(input, "个人邮箱:BUG #30246::邮件正文附件名称省略显示优化");
    }

    @Test
    public void decodeEncodedWords_withGB2312_decodes_correctly() {
        String input = "=?gb2312?B?Obv9t9az6cnu29rHsLqju6rHyLPHSlfN8rrAvsa16qOsuPzT0DIwvNIzOTnU?= " +
                "=?gb2312?B?qr6r0aG439DHytTLr77Gteq1yMTjwLSjoaOoQUSjqQ?=";
        assertInputDecodesToExpected(input, "9积分抽深圳前海华侨城JW万豪酒店，更有20家399元精选高星试睡酒店等你来！（AD�");
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
