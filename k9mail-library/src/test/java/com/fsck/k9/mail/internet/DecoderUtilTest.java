package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.K9LibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(K9LibRobolectricTestRunner.class)
public class DecoderUtilTest {

    private String body, expect;
    private MimeMessage message;

    @Test
    public void decodeEncodedWords_with_unencoded_data_returns_original_text() {
        body = "abc";
        expect = "abc";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withAsciiCharset_encoded_data_returns_text() {
        body = "=?us-ascii?q?abc?=";
        expect = "abc";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withStartOnly_encoding_format_returnAsText() {
        body = "=?";
        expect = "=?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withStartAndSeparatorOnly_returnAsText() {
        body = "=??";
        expect = "=??";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withStartAnd2SeparatorOnly_returnAsText() {
        body = "=???";
        expect = "=???";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withStartAnd3SeparatorOnly_returnAsText() {
        body = "=????";
        expect = "=????";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withSeparatorsOnly_returnAsText() {
        body = "=????=";
        expect = "=????=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withMissingCharset_returnAsText() {
        body = "=??q??=";
        expect = "=??q??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withTextAndMissingCharset_returnAsText() {

        body = "=??q?a?=";
        expect = "a";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withNoTextCharsetOrEncoding_returnAsText() {
        body = "=??=";
        expect = "=??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_with_MissingEncodingAndData_returnAsText() {
        body = "=?x?=";
        expect = "=?x?=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withMissingEncoding_returnAsText() {
        body = "=?x??=";
        expect = "=?x??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_with_incompleteEncodingFormat_returnAsText() {
        body = "=?x?q?=";
        expect = "=?x?q?=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_with_unrecognisedEncoding_withEmptyEncodedData_returnAsText() {
        body = "=?x?q??=";
        expect = "=?x?q??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withUnrecognisedEncoding_withEncodedData_return_encoded_data() {
        body = "=?x?q?X?=";
        expect = "X";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withInvalidBase64String_returnsEmptyString() {
        body = "=?us-ascii?b?abc?=";
        expect = "";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withPartiallyEncoded_returnsBothSections() {
        body = "=?us-ascii?q?abc?= =?";
        expect = "abc =?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withPartiallyEncodedAfter_returnsBothSections() {
        body = "def=?us-ascii?q?abc?=";
        expect = "defabc";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withUnrecognisedCharset_returnsEncodedData() {
        body = "=?x?= =?";
        expect = "=?x?= =?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withMultipleEncodedSections_decodesBoth() {
        body = "=?us-ascii?q?abc?= =?us-ascii?q?def?=";
        expect = "abcdef";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withMultipleEncodedSections_decodesSequentialSectionTogether() {
        //Splitting mid-character is RFC2047 non-compliant but seen in practice.
        body = "=?utf-8?B?5Liq5Lq66YKu566xOkJVRyAjMzAyNDY6OumCruS7tuato+aWh+mZhOS7tuWQ?=\n" +
                "=?utf-8?B?jeensOecgeeVpeaYvuekuuS8mOWMlg==?=";
        expect = "个人邮箱:BUG #30246::邮件正文附件名称省略显示优化";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }

    @Test
    public void decodeEncodedWords_withGB2312_decodes_correctly() {
        body = "=?gb2312?B?Obv9t9az6cnu29rHsLqju6rHyLPHSlfN8rrAvsa16qOsuPzT0DIwvNIzOTnU?= " +
                "=?gb2312?B?qr6r0aG439DHytTLr77Gteq1yMTjwLSjoaOoQUSjqQ?=";
        expect = "9积分抽深圳前海华侨城JW万豪酒店，更有20家399元精选高星试睡酒店等你来！（AD�";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }
}
