package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;

import org.xbill.DNS.MXRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


public class AutoconfigureMozilla implements AutoConfigure {
    @Override
    public ProviderInfo findProviderInfo(String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) return null;
        String domain = parts[1];

        DNSOperation dnsOperation = new DNSOperation();

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo providerInfo = autoconfigureISPDB.findProviderInfo(email);

        if (providerInfo != null) {
            return providerInfo;
        }

        try {
            MXRecord mxRecord = dnsOperation.mxLookup(domain);
            if (mxRecord != null) {
                final String target = mxRecord.getTarget().toString(true);
                final String[] targetParts = target.split("\\.");

                String newDomain = targetParts[targetParts.length - 2] + "." + targetParts[targetParts.length - 1];

                if (!newDomain.equals(domain)) {
                    providerInfo = autoconfigureISPDB.findProviderInfo(newDomain);
                }
            }
        } catch (TextParseException | UnknownHostException e) {
            Timber.e(e, "Error while trying to run MX lookup");
        }

        return providerInfo;
    }
}
