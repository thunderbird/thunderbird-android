package com.fsck.k9.mail.dns;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Test;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;

import static junit.framework.Assert.assertEquals;


public class DNSOperationTest {
    @Test
    public void mxLookup_returnsRecord() throws TextParseException, UnknownHostException {
        MXRecord record = new DNSOperation().mxLookup("gmail.com");

        assertEquals("gmail-smtp-in.l.google.com",
                record.getTarget().toString(true));
    }

    @Test
    public void srvLookup_returnsRecord() throws TextParseException, UnknownHostException {
        List<SRVRecord> records = new DNSOperation().srvLookup("_imaps._tcp.gmail.com");

        assertEquals(1, records.size());
        assertEquals("imap.gmail.com",
                records.get(0).getTarget().toString(true));
    }
}
