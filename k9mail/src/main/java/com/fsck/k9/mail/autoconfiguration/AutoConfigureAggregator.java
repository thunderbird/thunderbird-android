package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;


public class AutoConfigureAggregator {
    private final AutoconfigureMozilla autoconfigureMozilla;
    private final AutoconfigureSrv autoconfigureSrv;
    private final AutoConfigureAutodiscover autoconfigureAutodiscover;
    private final AutoconfigureGuesser autoconfigureGuesser;

    public AutoConfigureAggregator() {
        autoconfigureMozilla = new AutoconfigureMozilla();
        autoconfigureSrv = new AutoconfigureSrv();
        autoconfigureAutodiscover = new AutoConfigureAutodiscover();
        autoconfigureGuesser = new AutoconfigureGuesser();
    }

    public ProviderInfo findProviderInfo(String email) {
        ProviderInfo providerInfo = ProviderInfo.createEmpty();

        String providerDomain = EmailHelper.getDomainFromEmailAddress(email);

        // providerInfo = autoconfigureMozilla.findProviderInfo(email);
        // if (providerInfo != null) return providerInfo;

        providerInfo = autoconfigureSrv.findProviderInfo(providerInfo, email);
        if (providerInfo.isComplete()) {
            return providerInfo;
        }

//         providerInfo = autodiscover.findProviderInfo(email);
//         if (providerInfo != null) return providerInfo;

        // providerInfo = autoconfigureAutodiscover.findProviderInfo(email);
        // if (providerInfo != null) return providerInfo;

        providerInfo = autoconfigureGuesser.findProviderInfo(providerInfo, email);
        if (providerInfo.isComplete()) {
            return providerInfo;
        }

        return providerInfo;
    }

}
