package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;

import android.support.annotation.Nullable;

import com.fsck.k9.mail.ConnectionSecurity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import timber.log.Timber;


/**
 * Search in ISPDB
 */

public class AutoconfigureMozilla implements AutoConfigure {
    private static final String ISPDB_URL = "https://autoconfig.thunderbird.net/v1.1/%s";

    @Nullable
    public ProviderInfo parse(ProviderInfo providerInfo, Document document) {
        String incomingType;
        String incomingHost;
        Integer incomingPort;
        ConnectionSecurity incomingConnectionSecurity;
        String incomingUsernameTemplate;

        Element incomingElement = document.select("incomingServer").first();
        if (incomingElement == null) return null;
        Element incomingHostnameElement = incomingElement.select("hostname").first();
        if (incomingHostnameElement == null) return null;
        incomingHost = incomingHostnameElement.text();
        incomingType = incomingElement.attr("type").toLowerCase();
        Element incomingPortElement = incomingElement.select("port").first();
        if (incomingPortElement != null) {
            incomingPort = Integer.valueOf(incomingPortElement.text());
        }
        Element incomingSocketTypeElement = incomingElement.select("socketType").first();
        String incomingSocketType = incomingSocketTypeElement != null ?
                incomingSocketTypeElement.text().toLowerCase() :
                "";
        switch (incomingSocketType) {
            case "ssl":
                incomingConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
                break;
            case "starttls":
                incomingConnectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
                break;
            default:
                incomingConnectionSecurity = ConnectionSecurity.NONE;
                break;
        }
        final Element usernameElement = incomingElement.select("username").first();
        if (usernameElement == null) return null;
        final String incomingUsername = usernameElement.text();
        // "\\" to escape '$'
        incomingUsernameTemplate = incomingUsername
                .replaceAll("%EMAILDOMAIN%", "\\" + ProviderInfo.USERNAME_TEMPLATE_DOMAIN)
                .replaceAll("%EMAILADDRESS%", "\\" + ProviderInfo.USERNAME_TEMPLATE_EMAIL)
                .replaceAll("%EMAILLOCALPART%", "\\" + ProviderInfo.USERNAME_TEMPLATE_USER);
        // TODO
        // providerInfo = providerInfo.withXXX

        String outgoingType;
        String outgoingHost;
        Integer outgoingPort;
        ConnectionSecurity outgoingConnectionSecurity;
        String outgoingUsernameTemplate;

        Element outgoingElement = document.select("outgoingServer").first();
        final Element outgoingHostnameElement = outgoingElement.select("hostname").first();
        if (outgoingHostnameElement == null) return null;
        outgoingHost = outgoingHostnameElement.text();
        outgoingType = outgoingElement.attr("type").toLowerCase();
        Element outgoingPortElement = outgoingElement.select("port").first();
        if (outgoingPortElement != null) {
            outgoingPort = Integer.valueOf(outgoingPortElement.text());
        }
        final String outgoingSocketType = outgoingElement.select("socketType").first().text().toLowerCase();
        switch (outgoingSocketType) {
            case "ssl":
                outgoingConnectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
                break;
            case "starttls":
                outgoingConnectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
                break;
            default:
                outgoingConnectionSecurity = ConnectionSecurity.NONE;
                break;
        }
        final Element outgoingUsernameElement = outgoingElement.select("username").first();
        if (outgoingUsernameElement != null) {
            outgoingUsernameTemplate = outgoingUsernameElement.text()
                    .replaceAll("%EMAILDOMAIN%", "\\" + ProviderInfo.USERNAME_TEMPLATE_DOMAIN)
                    .replaceAll("%EMAILADDRESS%", "\\" + ProviderInfo.USERNAME_TEMPLATE_EMAIL)
                    .replaceAll("%EMAILLOCALPART%", "\\" + ProviderInfo.USERNAME_TEMPLATE_USER);
        }
        // TODO
        // providerInfo = providerInfo.withXXX

        return providerInfo;
    }

    @Override
    public ProviderInfo findProviderInfo(ProviderInfo providerInfo, String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) return null;
        String domain = parts[1];

        try {
            String url = String.format(ISPDB_URL, domain);
            Document document = Jsoup.connect(url).timeout(5000).get();

            return parse(providerInfo, document);
        } catch (IOException e) {
            Timber.w(e, "No information in ISPDB");
            return null;
        }
    }
}
