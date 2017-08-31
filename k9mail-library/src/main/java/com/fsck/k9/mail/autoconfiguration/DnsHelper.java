package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;

import org.xbill.DNS.MXRecord;
import org.xbill.DNS.TextParseException;


public class DnsHelper {
    public static String getMxDomain(String domain) throws UnknownHostException {
        DNSOperation dnsOperation = new DNSOperation();
        MXRecord mxRecord;
        try {
            mxRecord = dnsOperation.mxLookup(domain);
        } catch (TextParseException e) {
            return null;
        }
        if (mxRecord != null) {
            final String target = mxRecord.getTarget().toString(true);
            return getDomainFromFqdn(target);
        }
        return null;
    }

    private static String getDomainFromFqdn(String fqdn) {
        final String[] parts = fqdn.split("\\.");

        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
