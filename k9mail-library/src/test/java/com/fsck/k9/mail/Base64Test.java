package com.fsck.k9.mail;

import com.fsck.k9.mail.filter.Base64;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base64Test {
    @Test
    public void encode_Base64() {
        String s = "Something";
        String b64 = Base64.encode(s);
        String s2 = Base64.decode(b64);
        assertEquals(s, s2);
    }
    @Test
    public void encode_Base64null() {
        String s = null;
        assertEquals(s, Base64.decode(Base64.encode(s)));
    }
}