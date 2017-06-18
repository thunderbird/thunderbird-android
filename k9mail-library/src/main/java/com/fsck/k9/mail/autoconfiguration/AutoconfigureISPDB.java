package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import timber.log.Timber;


/**
 * Search in ISPDB
 */

public class AutoconfigureISPDB implements AutoConfigure {
    private static final String ISPDB_URL = "https://autoconfig.thunderbird.net/v1.1/%s";

    @Override
    public ProviderInfo findProviderInfo(String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) return null;
        String domain = parts[1];

        try {
            ProviderInfo providerInfo = new ProviderInfo();

            String url = String.format(ISPDB_URL, domain);
            Document document = Jsoup.connect(url).get();

            Elements incomingEles = document.select("incomingServer");
            Element incoming = incomingEles.first();

            if (incoming == null) return null;

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

        } catch (IOException e) {
            Timber.w(e, "No information in ISPDB");
            return null;
        }
    }
}
