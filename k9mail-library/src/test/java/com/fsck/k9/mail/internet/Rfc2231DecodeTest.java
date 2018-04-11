package com.fsck.k9.mail.internet;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Rfc2231DecodeTest {
    @Test
    public void decodeAll_rfc2231_example_1()
    {
        // From RFC2231 3.
        // this is Content-Type, not Content-Disposition, but same logic applies.
        String source = "Content-Type: message/external-body; access-type=URL;\n" +
                "         URL*0=\"ftp://\";\n" +
                "         URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"";
        String decoded = Rfc2231Parameters.decodeAll(source, null);
        String output = MimeUtility.getHeaderParameter(decoded, "URL");
        assertEquals("ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar",output);
    }

    @Test
    public void decodeAll_rfc2231_example_2()
    {
        // From RFC2231 4.
        String source = "Content-Type: application/x-stuff;\n" +
                "         title*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A";
        String decoded = Rfc2231Parameters.decodeAll(source, null);
        String output = MimeUtility.getHeaderParameter(decoded, "title");
        assertEquals("This is ***fun***",output);
    }

    @Test
    public void decodeAll_rfc2231_example_3()
    {
        // From RFC2231 4.1
        String source = "Content-Type: application/x-stuff;\n" +
                "    title*0*=us-ascii'en'This%20is%20even%20more%20;\n" +
                "    title*1*=%2A%2A%2Afun%2A%2A%2A%20;\n" +
                "    title*2=\"isn't it!\"";
        String decoded = Rfc2231Parameters.decodeAll(source, null);
        String output = MimeUtility.getHeaderParameter(decoded, "title");
        assertEquals("This is even more ***fun*** isn't it!",output);
    }

    @Test
    public void decodeAll_thunderbird_Japanese_decode()
    {
        String source = "Content-Disposition: attachment;\n" +
                "filename*0*=utf-8''%61%6C%70%68%61%E5%9B%B3%E9%9D%A2%E5%9F%BA%E6%9C%AC%E6;\n" +
                "filename*1*=%A1%88%EF%BC%8D%E6%A4%9C%E8%A8%8E%E4%B8%AD%E3%83%BB%E6%A1%88;\n" +
                "filename*2*=%42%32%2E%74%78%74";
        String decoded = Rfc2231Parameters.decodeAll(source, null);
        String name = MimeUtility.getHeaderParameter(decoded, "filename");
        assertEquals("alpha図面基本案－検討中・案B2.txt",name);
    }

    @Test
    public void deodeAll_appleMail_Japanese_decode()
    {
        String source = "Content-Disposition: attachment;\n" +
                "\tfilename*=utf-8''0409%E6%B4%BB%E5%8B%95%E5%A0%B1%E5%91%8A%E6%9B%B8%EF%BC%88%E6%94%B9%E8%A8%822%E7%89%88%EF%BC%89.txt\n";
        String decoded = Rfc2231Parameters.decodeAll(source, null);
        String name = MimeUtility.getHeaderParameter(decoded, "filename");
        assertEquals("0409活動報告書（改訂2版）.txt",name);
    }
}
