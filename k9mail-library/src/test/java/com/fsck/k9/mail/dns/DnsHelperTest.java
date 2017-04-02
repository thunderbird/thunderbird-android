package com.fsck.k9.mail.dns;


import java.net.UnknownHostException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class DnsHelperTest {

    @Test
    public void getMxDomain_returnsMxDomain() throws UnknownHostException {
        assertEquals("google.com", DnsHelper.getMxDomain("google.com"));
        assertEquals("google.com", DnsHelper.getMxDomain("gmail.com"));
    }

    @Test
    public void getMxDomain_returnsNullForDomainWithNoMxRecord() throws UnknownHostException {
        assertNull(DnsHelper.getMxDomain("example.com"));
    }
}
