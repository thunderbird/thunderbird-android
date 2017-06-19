package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


/**
 * According to RFC 6186
 */

public class AutoconfigureSrv implements AutoConfigure {
    public ProviderInfo parse(List<SRVRecord> imapRecords, List<SRVRecord> pop3Records,
            List<SRVRecord> submissionRecords) {
        ProviderInfo providerInfo = new ProviderInfo();

        DNSOperation dnsOperation = new DNSOperation();

        SRVRecord imapRecord = dnsOperation.choose(imapRecords);
        SRVRecord pop3Record = dnsOperation.choose(pop3Records);

        SRVRecord incomingRecord;
        if (imapRecord != null && pop3Record != null) {
            incomingRecord = imapRecord.getPriority() <= pop3Record.getPriority() ? imapRecord : pop3Record;
        } else if (imapRecord != null) {
            incomingRecord = imapRecord;
        } else if (pop3Record != null) {
            incomingRecord = pop3Record;
        } else {
            return null;
        }

        providerInfo.incomingPort = incomingRecord.getPort();

        providerInfo.incomingAddr = incomingRecord.getTarget().toString(true);
        if (incomingRecord.getName().toString().startsWith("_imaps")) {
            providerInfo.incomingSocketType = "ssl";
            providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        } else if (incomingRecord.getName().toString().startsWith("_imap")) {
            providerInfo.incomingSocketType = "tls";
            providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        } else if (incomingRecord.getName().toString().startsWith("_pop3s")) {
            providerInfo.incomingSocketType = "ssl";
            providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        } else if (incomingRecord.getName().toString().startsWith("_pop3")) {
            providerInfo.incomingSocketType = "tls";
            providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        }

        SRVRecord outgoingRecord = dnsOperation.choose(submissionRecords);
        if (outgoingRecord != null) {
            providerInfo.outgoingAddr = outgoingRecord.getTarget().toString(true);
            providerInfo.outgoingSocketType = "tls";
            providerInfo.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
            providerInfo.outgoingPort = outgoingRecord.getPort();
        } else {
            return null;
        }

        providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;

        return providerInfo;
    }

    @Override
    public ProviderInfo findProviderInfo(String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) return null;
        String domain = parts[1];

        DNSOperation dnsOperation = new DNSOperation();

        ProviderInfo providerInfo;
        try {
            List<SRVRecord> imapRecords = new ArrayList<>();
            imapRecords.addAll(dnsOperation.srvLookup("_imaps._tcp." + domain));
            imapRecords.addAll(dnsOperation.srvLookup("_imap._tcp." + domain));

            List<SRVRecord> pop3Records = new ArrayList<>();
            pop3Records.addAll(dnsOperation.srvLookup("_pop3s._tcp." + domain));
            pop3Records.addAll(dnsOperation.srvLookup("_pop3._tcp." + domain));

            List<SRVRecord> outgoingRecords = dnsOperation.srvLookup("_submission._tcp." + domain);

            providerInfo = parse(imapRecords, pop3Records, outgoingRecords);

        } catch (TextParseException e) {
            Timber.e(e, "Error while trying to do SRV lookup");
            return null;
        } catch (UnknownHostException e) {
            Timber.w(e, "No valid SRV record for " + domain);
            return null;
        }

        return providerInfo;
    }
}
