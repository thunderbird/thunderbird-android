package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
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
    public static ProviderInfo findProviderInISPDB(String domain) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://autoconfig.thunderbird.net/v1.1/" + domain)
                .build();
        try {
            Response response = client.newCall(request).execute();
            ProviderInfo providerInfo = new ProviderInfo();

            Document document = Jsoup.parse(response.body().string(), "", Parser.xmlParser());

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

        // TODO: 17-4-2 how to auto detect it?
        providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_UNKNOWN;
        return providerInfo;
    }

    private static SRVRecord srvLookup(String serviceName) throws TextParseException, UnknownHostException {
        Lookup lookup = new Lookup(serviceName, Type.SRV, DClass.IN);
        Resolver resolver = new SimpleResolver();
        lookup.setResolver(resolver);
        lookup.setCache(null);
        Record[] records = lookup.run();

        List<SRVRecord> res = new ArrayList<>();

        if (lookup.getResult() == Lookup.SUCCESSFUL) {
            for (Record record : records) {
                if (record instanceof SRVRecord) {
                    res.add((SRVRecord) record);
                }
            }
        }

        // TODO: 17-4-2 return record with max priority
        if (res.size() > 0) {
            return res.get(0);
        }
        return null;
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
