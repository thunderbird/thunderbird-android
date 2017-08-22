package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;

import org.xbill.DNS.MXRecord;
import org.xbill.DNS.TextParseException;


public class DNSHelper {
    public static String getMXDomain(String domain) throws TextParseException, UnknownHostException {
        DNSOperation dnsOperation = new DNSOperation();
        MXRecord mxRecord = dnsOperation.mxLookup(domain);
        if (mxRecord != null) {
            final String target = mxRecord.getTarget().toString(true);
            final String[] targetParts = target.split("\\.");

            return targetParts[targetParts.length - 2] + "." + targetParts[targetParts.length - 1];
        }
        return null;
    }
}
