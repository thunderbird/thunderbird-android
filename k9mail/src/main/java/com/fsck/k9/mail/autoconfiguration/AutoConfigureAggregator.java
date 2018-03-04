package com.fsck.k9.mail.autoconfiguration;


import java.net.UnknownHostException;

import android.content.Context;

import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;


public class AutoConfigureAggregator {

    public ProviderInfo findProviderInfo(Context context, String email) {
        String mailDomain = EmailHelper.getDomainFromEmailAddress(email);

        ProviderInfo providerInfo;
        AutoconfigureMozilla autoconfigureMozilla = new AutoconfigureMozilla();
        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();
        AutoconfigureGuesser guesser = new AutoconfigureGuesser(context);

        providerInfo = autoconfigureMozilla.findProviderInfo(email);
        if (providerInfo != null) return providerInfo;

        providerInfo = autoconfigureSrv.findProviderInfo(email);
        if (providerInfo != null) return providerInfo;

        // providerInfo = autodiscover.findProviderInfo(email);
        // if (providerInfo != null) return providerInfo;

        providerInfo = autodiscover.findProviderInfo(email);
        if (providerInfo != null) return providerInfo;

        providerInfo = guesser.findProviderInfo(email);
        if (providerInfo != null) return providerInfo;

        try {
            String mxDomain = DnsHelper.getMxDomain(email);
        } catch (UnknownHostException e) {
            return null;
        }

        return providerInfo;
    }

}
