package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;

import android.support.annotation.Nullable;

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

    @Nullable
    public ProviderInfo parse(Document document) {
        ProviderInfo providerInfo = new ProviderInfo();

        Element incomingElement = document.select("incomingServer").first();
        if (incomingElement == null) return null;
        Element incomingHostnameElement = incomingElement.select("hostname").first();
        if (incomingHostnameElement == null) return null;
        providerInfo.incomingAddr = incomingHostnameElement.text();
        providerInfo.incomingType = incomingElement.attr("type").toLowerCase();
        Element incomingPortElement = incomingElement.select("port").first();
        if (incomingPortElement != null) providerInfo.incomingPort = Integer.valueOf(incomingPortElement.text());
        Element incomingSocketTypeElement = incomingElement.select("socketType").first();
        String incomingSocketType = incomingSocketTypeElement != null ?
                incomingSocketTypeElement.text().toLowerCase() :
                "";
        switch (incomingSocketType) {
            case "ssl":
                providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
                break;
            case "starttls":
                providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
                break;
            default:
                providerInfo.incomingSocketType = "";
                break;
        }
        final Element usernameElement = incomingElement.select("username").first();
        if (usernameElement == null) return null;
        final String incomingUsername = usernameElement.text();
        // "\\" to escape '$'
        providerInfo.incomingUsernameTemplate = incomingUsername
                .replaceAll("%EMAILDOMAIN%", "\\" + ProviderInfo.USERNAME_TEMPLATE_DOMAIN)
                .replaceAll("%EMAILADDRESS%", "\\" + ProviderInfo.USERNAME_TEMPLATE_EMAIL)
                .replaceAll("%EMAILLOCALPART%", "\\" + ProviderInfo.USERNAME_TEMPLATE_USER);

        Element outgoingElement = document.select("outgoingServer").first();
        final Element outgoingHostnameElement = outgoingElement.select("hostname").first();
        if (outgoingHostnameElement == null) return null;
        providerInfo.outgoingAddr = outgoingHostnameElement.text();
        providerInfo.outgoingType = outgoingElement.attr("type").toLowerCase();
        Element outgoingPortElement = outgoingElement.select("port").first();
        if (outgoingPortElement != null) providerInfo.outgoingPort = Integer.valueOf(outgoingPortElement.text());
        final String outgoingSocketType = outgoingElement.select("socketType").first().text().toLowerCase();
        switch (outgoingSocketType) {
            case "ssl":
                providerInfo.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
                break;
            case "starttls":
                providerInfo.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
                break;
            default:
                providerInfo.outgoingSocketType = "";
                break;
        }
        final Element outgoingUsernameElement = outgoingElement.select("username").first();
        if (outgoingUsernameElement != null) {
            providerInfo.outgoingUsernameTemplate = outgoingUsernameElement.text()
                    .replaceAll("%EMAILDOMAIN%", "\\" + ProviderInfo.USERNAME_TEMPLATE_DOMAIN)
                    .replaceAll("%EMAILADDRESS%", "\\" + ProviderInfo.USERNAME_TEMPLATE_EMAIL)
                    .replaceAll("%EMAILLOCALPART%", "\\" + ProviderInfo.USERNAME_TEMPLATE_USER);
        }
        return providerInfo;
    }

    @Override
    public ProviderInfo findProviderInfo(String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) return null;
        String domain = parts[1];

        try {
            String url = String.format(ISPDB_URL, domain);
            Document document = Jsoup.connect(url).get();

            return parse(document);
        } catch (IOException e) {
            Timber.w(e, "No information in ISPDB");
            return null;
        }
    }
}
