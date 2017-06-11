package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
 * Created by daquexian on 6/9/17.
 * Util class for autoconfiguration
 */

public class AutoConfigurationUtil {
    public static ProviderInfo findProviderMozilla(String domain) {
        ProviderInfo providerInfo = findProviderInISPDB(domain);

        if (providerInfo != null) {
            return providerInfo;
        }

        try {
            MXRecord mxRecord = mxLookup(domain);
            if (mxRecord != null) {
                final String target = mxRecord.getTarget().toString(true);
                final String[] targetParts = target.split("\\.");

                String newDomain = targetParts[targetParts.length - 2] + "." + targetParts[targetParts.length - 1];

                if (!newDomain.equals(domain)) {
                    providerInfo = findProviderInISPDB(newDomain);
                }
            }
        } catch (Exception e) {

        }

        return providerInfo;
    }

    private static ProviderInfo findProviderInISPDB(String domain) {
        try {
            ProviderInfo providerInfo = new ProviderInfo();

            Document document = Jsoup.connect("https://autoconfig.thunderbird.net/v1.1/" + domain).get();

            Elements incomingEles = document.select("incomingServer");
            Element incoming = incomingEles.first();
            providerInfo.incomingAddr = incoming.select("hostname").first().text();
            providerInfo.incomingType = incoming.attr("type").toLowerCase();
            providerInfo.incomingSocketType = incoming.select("socketType").first().text().toLowerCase();

            switch (incoming.select("username").first().text()) {
                case "%EMAILADDRESS%":
                    providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
                    break;
                case "%EMAILLOCALPART%":
                    providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
                    break;
                default:
                    break;
            }

            Element outgoing = document.select("outgoingServer").first();
            providerInfo.outgoingAddr = outgoing.select("hostname").first().text();
            providerInfo.outgoingType = outgoing.attr("type").toLowerCase();
            providerInfo.outgoingSocketType = outgoing.select("socketType").first().text().toLowerCase();
            switch (outgoing.select("username").first().text()) {
                case "%EMAILADDRESS%":
                    providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
                    break;
                case "%EMAILLOCALPART%":
                    providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
                    break;
                default:
                    break;
            }
            return providerInfo;

        } catch (Exception e) {
            return null;
        }
    }

    public static ProviderInfo findProviderBySrv(String domain) {
        ProviderInfo providerInfo = new ProviderInfo();
        try {
            SRVRecord srvRecord = srvLookup("_imaps._tcp." + domain);
            if (srvRecord != null && !srvRecord.getTarget().toString().equals(".")) {
                providerInfo.incomingAddr = srvRecord.getTarget().toString(true);
                // TODO: 17-4-2 any better way to detect ssl/tls?
                providerInfo.incomingSocketType = srvRecord.getPort() == 993 ? "ssl" : "tls";
                providerInfo.incomingType = "imap";
            } else {
                srvRecord = srvLookup("_imap._tcp." + domain);

                if (srvRecord != null && !srvRecord.getTarget().toString().equals(".")) {
                    providerInfo.incomingAddr = srvRecord.getTarget().toString(true);
                    providerInfo.incomingSocketType = "";
                    providerInfo.incomingType = "imap";
                } else {
                    return null;
                }
            }

            srvRecord = srvLookup("_submission._tcp." + domain);
            if (srvRecord != null && !srvRecord.getTarget().toString().equals(".")) {
                providerInfo.outgoingAddr = srvRecord.getTarget().toString(true);
                // TODO: 17-4-2 any better way to detect ssl/tls?
                switch (srvRecord.getPort()) {
                    case 465:
                        providerInfo.outgoingSocketType = "ssl";
                        break;
                    case 587:
                        providerInfo.outgoingSocketType = "tls";
                        break;
                    default:
                        providerInfo.outgoingSocketType = "";
                        break;
                }
                providerInfo.outgoingType = "stmp";
            } else {
                return null;
            }

        } catch (TextParseException | UnknownHostException e) {
            return null;
        }

        providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_UNKNOWN;
        providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_UNKNOWN;
        return providerInfo;
    }

    private static MXRecord mxLookup(String domain) throws TextParseException, UnknownHostException {
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

    private static SRVRecord srvLookup(String serviceName) throws TextParseException, UnknownHostException {
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

    public static class ProviderInfo {
        public String incomingUsernameTemplate;

        public String outgoingUsernameTemplate;

        public String incomingType;
        public String incomingSocketType;
        public String incomingAddr;
        public String outgoingType;
        public String outgoingSocketType;
        public String outgoingAddr;

        public static String USERNAME_TEMPLATE_EMAIL = "email";
        public static String USERNAME_TEMPLATE_USER = "username";
        public static String USERNAME_TEMPLATE_UNKNOWN = "unknown";
    }
}
