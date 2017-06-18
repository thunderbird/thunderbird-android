package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;

import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


/**
 * According to RFC 6186
 */

public class AutoconfigureSrv implements AutoConfigure {
    @Override
    public ProviderInfo findProviderInfo(String domain) {
        DNSOperation dnsOperation = new DNSOperation();

        ProviderInfo providerInfo = new ProviderInfo();
        try {
            SRVRecord srvRecord = dnsOperation.srvLookup("_imaps._tcp." + domain);
            if (srvRecord != null && !srvRecord.getTarget().toString().equals(".")) {
                providerInfo.incomingAddr = srvRecord.getTarget().toString(true);
                // TODO: 17-4-2 any better way to detect ssl/tls?
                providerInfo.incomingSocketType = srvRecord.getPort() == 993 ? "ssl" : "tls";
                providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
            } else {
                srvRecord = dnsOperation.srvLookup("_imap._tcp." + domain);

                if (srvRecord != null && !srvRecord.getTarget().toString().equals(".")) {
                    providerInfo.incomingAddr = srvRecord.getTarget().toString(true);
                    providerInfo.incomingSocketType = "";
                    providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
                } else {
                    return null;
                }
            }

            srvRecord = dnsOperation.srvLookup("_submission._tcp." + domain);
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
            Timber.e(e, "Error while trying to do SRV lookup");
            return null;
        }

        providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_UNKNOWN;
        providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_UNKNOWN;
        return providerInfo;
    }
}
