package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.K9LibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(K9LibRobolectricTestRunner.class)
public class DecoderUtilTest {

    @Test
    public void testDecodeEncodedWords() {
        String body, expect;
        MimeMessage message;

        body = "abc";
        expect = "abc";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?us-ascii?q?abc?=";
        expect = "abc";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?";
        expect = "=?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=??";
        expect = "=??";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=???";
        expect = "=???";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=????";
        expect = "=????";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=????=";
        expect = "=????=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=??q??=";
        expect = "=??q??=";
        ;
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=??q?a?=";
        expect = "a";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=??=";
        expect = "=??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x?=";
        expect = "=?x?=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x??=";
        expect = "=?x??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x?q?=";
        expect = "=?x?q?=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x?q??=";
        expect = "=?x?q??=";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x?q?X?=";
        expect = "X";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        // invalid base64 string
        body = "=?us-ascii?b?abc?=";
        expect = "";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        // broken encoded header
        body = "=?us-ascii?q?abc?= =?";
        expect = "abc =?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        body = "=?x?= =?";
        expect = "=?x?= =?";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        //Multi encoded header
        body = "=?utf-8?B?5Liq5Lq66YKu566xOkJVRyAjMzAyNDY6OumCruS7tuato+aWh+mZhOS7tuWQ?=\n" +
                "=?utf-8?B?jeensOecgeeVpeaYvuekuuS8mOWMlg==?=";
        expect = "个人邮箱:BUG #30246::邮件正文附件��称省略显示优化";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));

        //Non utf-8 encoding
        body = "=?gb2312?B?Obv9t9az6cnu29rHsLqju6rHyLPHSlfN8rrAvsa16qOsuPzT0DIwvNIzOTnU?= " +
                "=?gb2312?B?qr6r0aG439DHytTLr77Gteq1yMTjwLSjoaOoQUSjqQ?=";
        expect = "9积分抽深圳前海华侨城JW万豪酒店，更有20家399��精选高星试睡酒店等你来！（AD�";
        message = null;
        assertEquals(expect, DecoderUtil.decodeEncodedWords(body, message));
    }
}
