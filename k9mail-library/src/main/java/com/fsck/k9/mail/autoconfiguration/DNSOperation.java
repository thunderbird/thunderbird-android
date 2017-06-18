package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.Arrays;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


/**
 * Util class for DNS operations
 */

class DNSOperation {
    MXRecord mxLookup(String domain) throws TextParseException, UnknownHostException {
        Lookup lookup = new Lookup(domain, Type.MX, DClass.IN);
        Resolver resolver = new SimpleResolver();
        lookup.setResolver(resolver);
        lookup.setCache(null);
        Record[] records = lookup.run();
        MXRecord[] mxRecords = Arrays.copyOf(records, records.length, MXRecord[].class);

        MXRecord res = null;
        if (lookup.getResult() == Lookup.SUCCESSFUL) {
            for (MXRecord record : mxRecords) {
                if (res == null || record.getPriority() < res.getPriority()) {
                    res = record;
                }
            }
        }

        return res;
    }

    SRVRecord srvLookup(String serviceName) throws TextParseException, UnknownHostException {
        Lookup lookup = new Lookup(serviceName, Type.SRV, DClass.IN);
        Resolver resolver = new SimpleResolver();
        lookup.setResolver(resolver);
        lookup.setCache(null);

        Record[] records = lookup.run();
        SRVRecord[] srvRecords = Arrays.copyOf(records, records.length, SRVRecord[].class);

        SRVRecord res = null;

        if (lookup.getResult() == Lookup.SUCCESSFUL) {
            for (SRVRecord record : srvRecords) {
                if (res == null || record.getPriority() < res.getPriority() ||
                        (record.getPriority() == res.getPriority() && record.getWeight() > res.getWeight())) {
                    res = record;
                }
            }
        }
        return res;
    }
}
