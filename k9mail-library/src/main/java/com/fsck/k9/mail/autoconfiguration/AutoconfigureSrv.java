package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.ConnectionSecurity;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


/**
 * According to RFC 6186
 */

public class AutoconfigureSrv implements AutoConfigure {

    public ProviderInfo parse(ProviderInfo providerInfo,
            List<SRVRecord> imapRecords, List<SRVRecord> pop3Records, List<SRVRecord> submissionRecords) {
        DnsOperation dnsOperation = new DnsOperation();

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
            return providerInfo;
        }

        String incomingAddr = incomingRecord.getTarget().toString(true);
        int incomingPort = incomingRecord.getPort();

        if (incomingRecord.getName().toString().startsWith("_imaps")) {
            providerInfo = providerInfo.withImapInfo(incomingAddr, incomingPort, ConnectionSecurity.SSL_TLS_REQUIRED);
        } else if (incomingRecord.getName().toString().startsWith("_imap")) {
            providerInfo = providerInfo.withImapInfo(incomingAddr, incomingPort, ConnectionSecurity.STARTTLS_REQUIRED);
        } else if (incomingRecord.getName().toString().startsWith("_pop3s")) {
            providerInfo = providerInfo.withPop3Info(incomingAddr, incomingPort, ConnectionSecurity.SSL_TLS_REQUIRED);
        } else if (incomingRecord.getName().toString().startsWith("_pop3")) {
            providerInfo = providerInfo.withPop3Info(incomingAddr, incomingPort, ConnectionSecurity.STARTTLS_REQUIRED);
        }

        SRVRecord outgoingRecord = dnsOperation.choose(submissionRecords);
        if (outgoingRecord != null) {
            String submissionAddr = outgoingRecord.getTarget().toString(true);
            int submissionPort = outgoingRecord.getPort();
            providerInfo = providerInfo.withSmtpInfo(submissionAddr, submissionPort, ConnectionSecurity.STARTTLS_REQUIRED);
        }

        return providerInfo;
    }

    @Override
    public ProviderInfo findProviderInfo(ProviderInfo providerInfo, String localpart, String domain) {
        DnsOperation dnsOperation = new DnsOperation();

        try {
            List<SRVRecord> imapRecords = new ArrayList<>();
            imapRecords.addAll(dnsOperation.srvLookup("_imaps._tcp." + domain));
            imapRecords.addAll(dnsOperation.srvLookup("_imap._tcp." + domain));

            List<SRVRecord> pop3Records = new ArrayList<>();
            pop3Records.addAll(dnsOperation.srvLookup("_pop3s._tcp." + domain));
            pop3Records.addAll(dnsOperation.srvLookup("_pop3._tcp." + domain));

            List<SRVRecord> outgoingRecords = dnsOperation.srvLookup("_submission._tcp." + domain);

            providerInfo = parse(providerInfo, imapRecords, pop3Records, outgoingRecords);
        } catch (TextParseException e) {
            Timber.e(e, "Error while trying to do SRV lookup");
        } catch (UnknownHostException e) {
            Timber.w(e, "No valid SRV record for " + domain);
        }

        return providerInfo;
    }
}
