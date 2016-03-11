package com.fsck.k9.ssl;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class CertificateErrorUtilsTest {

    @Test
    public void can_parse_hostname_exception() {
        ArrayList<String> hostnames = CertificationErrorUtils.extractHostnames(
                "hostname in certificate didn't match: <webmail.whiuk.com> != <mail.whiuk.com> OR <mail.whiuk.com> OR <whiuk.com>");
        assertEquals(3, hostnames.size());
        assertEquals("webmail.whiuk.com", hostnames.get(0));
        assertEquals("mail.whiuk.com", hostnames.get(1));
        assertEquals("whiuk.com", hostnames.get(2));
    }
}
