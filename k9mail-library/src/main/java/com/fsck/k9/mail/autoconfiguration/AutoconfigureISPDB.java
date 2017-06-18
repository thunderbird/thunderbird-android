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
    @Override
    public ProviderInfo findProviderInfo(String domain) {
        try {
            ProviderInfo providerInfo = new ProviderInfo();

            Document document = Jsoup.connect("https://autoconfig.thunderbird.net/v1.1/" + domain).get();

            Elements incomingEles = document.select("incomingServer");
            Element incoming = incomingEles.first();

            // FIXME: 6/14/17 Can incoming be null?
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
