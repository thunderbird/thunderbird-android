package com.fsck.k9.autodiscovery.providersxml;


import java.net.URI;
import java.net.URISyntaxException;

import android.content.res.XmlResourceParser;

import com.fsck.k9.autodiscovery.ConnectionSettings;
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery;
import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.ServerSettings;
import org.xmlpull.v1.XmlPullParser;
import timber.log.Timber;


public class ProvidersXmlDiscovery implements ConnectionSettingsDiscovery {
    private final BackendManager backendManager;
    private final ProvidersXmlProvider xmlProvider;


    public ProvidersXmlDiscovery(BackendManager backendManager, ProvidersXmlProvider xmlProvider) {
        this.backendManager = backendManager;
        this.xmlProvider = xmlProvider;
    }

    @Override
    public ConnectionSettings discover(String email) {
        String password = "";

        String user = EmailHelper.getLocalPartFromEmailAddress(email);
        String domain = EmailHelper.getDomainFromEmailAddress(email);
        if (user == null || domain == null) {
            return null;
        }

        Provider mProvider = findProviderForDomain(domain);
        if (mProvider == null) {
            return null;
        }
        try {
            String userEnc = UrlEncodingHelper.encodeUtf8(user);
            String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

            String incomingUsername = mProvider.incomingUsernameTemplate;
            incomingUsername = incomingUsername.replaceAll("\\$email", email);
            incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
            incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

            URI incomingUriTemplate = mProvider.incomingUriTemplate;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUsername + ":" + passwordEnc,
                    incomingUriTemplate.getHost(), incomingUriTemplate.getPort(), null, null, null);

            String outgoingUsername = mProvider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = mProvider.outgoingUriTemplate;


            URI outgoingUri;
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
                outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
                outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);
                outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUsername + ":"
                        + passwordEnc, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                        null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                        null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                        null, null);
            }

            ServerSettings incomingSettings = backendManager.decodeStoreUri(incomingUri.toString());
            ServerSettings outgoingSettings = backendManager.decodeTransportUri(outgoingUri.toString());
            return new ConnectionSettings(incomingSettings, outgoingSettings);
        } catch (URISyntaxException use) {
            return null;
        }
    }

    private Provider findProviderForDomain(String domain) {
        try {
            XmlResourceParser xml = xmlProvider.getXml();
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlPullParser.END_DOCUMENT) {
                if (xmlEventType == XmlPullParser.START_TAG &&
                        "provider".equals(xml.getName()) &&
                        domain.equalsIgnoreCase(xml.getAttributeValue(null, "domain"))) {
                    provider = new Provider();
                    provider.id = xml.getAttributeValue(null, "id");
                    provider.label = xml.getAttributeValue(null, "label");
                    provider.domain = xml.getAttributeValue(null, "domain");
                } else if (xmlEventType == XmlPullParser.START_TAG &&
                        "incoming".equals(xml.getName()) &&
                        provider != null) {
                    provider.incomingUriTemplate = new URI(xml.getAttributeValue(null, "uri"));
                    provider.incomingUsernameTemplate = xml.getAttributeValue(null, "username");
                } else if (xmlEventType == XmlPullParser.START_TAG &&
                        "outgoing".equals(xml.getName()) &&
                        provider != null) {
                    provider.outgoingUriTemplate = new URI(xml.getAttributeValue(null, "uri"));
                    provider.outgoingUsernameTemplate = xml.getAttributeValue(null, "username");
                } else if (xmlEventType == XmlPullParser.END_TAG &&
                        "provider".equals(xml.getName()) &&
                        provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error while trying to load provider settings.");
        }
        return null;
    }


    static class Provider {
        String id;
        String label;
        String domain;
        URI incomingUriTemplate;
        String incomingUsernameTemplate;
        URI outgoingUriTemplate;
        String outgoingUsernameTemplate;
    }
}
