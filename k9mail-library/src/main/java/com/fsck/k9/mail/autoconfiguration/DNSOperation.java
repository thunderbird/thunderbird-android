package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        if (records == null) return null;

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

    @NonNull
    List<SRVRecord> srvLookup(String serviceName) throws TextParseException, UnknownHostException {
        Lookup lookup = new Lookup(serviceName, Type.SRV, DClass.IN);
        Resolver resolver = new SimpleResolver();
        lookup.setResolver(resolver);
        lookup.setCache(null);

        List<SRVRecord> res = new ArrayList<>();

        Record[] records = lookup.run();
        if (records == null) return res;

        SRVRecord[] srvRecords = Arrays.copyOf(records, records.length, SRVRecord[].class);

        if (lookup.getResult() == Lookup.SUCCESSFUL) {
            res = Arrays.asList(srvRecords);
        }

        return res;
    }

    @Nullable
    SRVRecord choose(@NonNull List<SRVRecord> srvRecords) {
        ArrayList<SRVRecord> recordsWithLowestPriority = new ArrayList<>();
        int lowestPriority = -1;
        int totalWeights = 0;

        for (SRVRecord record : srvRecords) {
            if ((lowestPriority == -1 || lowestPriority > record.getPriority()) &&
                    !record.getTarget().toString().equals(".")) {
                lowestPriority = record.getPriority();
            }
        }

        for (SRVRecord record : srvRecords) {
            if (record.getPriority() == lowestPriority) {
                recordsWithLowestPriority.add(record);
                totalWeights += record.getWeight();
            }
        }

        if (recordsWithLowestPriority.size() == 1) {
            return recordsWithLowestPriority.get(0);
        }

        int randomNum = (int) Math.round(totalWeights * Math.random());
        for (SRVRecord record : recordsWithLowestPriority) {
            randomNum -= record.getWeight();
            if (randomNum <= 0) {
                return record;
            }
        }

        return null;
    }
}
