package com.fsck.k9.autodiscovery;

import android.app.Application;
import android.content.res.XmlResourceParser;

import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.ui.R;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import timber.log.Timber;

public class ProvidersXmlDiscovery implements ConnectionSettingsDiscovery {

    private final BackendManager backendManager;

    private final Application application;

    public ProvidersXmlDiscovery(BackendManager backendManager, Application application) {
        this.backendManager = backendManager;
        this.application = application;
    }

    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }

    @Override
    public ConnectionSettings discover(String email) {
        String password = "";
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
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
            XmlResourceParser xml = application.getResources().getXml(R.xml.providers);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())
                        && domain.equalsIgnoreCase(getXmlAttribute(xml, "domain"))) {
                    provider = new Provider();
                    provider.id = getXmlAttribute(xml, "id");
                    provider.label = getXmlAttribute(xml, "label");
                    provider.domain = getXmlAttribute(xml, "domain");
                    provider.note = getXmlAttribute(xml, "note");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                        && "incoming".equals(xml.getName())
                        && provider != null) {
                    provider.incomingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                        && "outgoing".equals(xml.getName())
                        && provider != null) {
                    provider.outgoingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.END_TAG
                        && "provider".equals(xml.getName())
                        && provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error while trying to load provider settings.");
        }
        return null;
    }

    @Override
    public String toString() {
        return "Internal providers.xml";
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     * @param xml
     * @param name
     * @return
     */
    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return application.getString(resId);
        }
    }

    static class Provider implements Serializable {
        private static final long serialVersionUID = 8511656164616538989L;

        public String id;

        public String label;

        public String domain;

        public URI incomingUriTemplate;

        public String incomingUsernameTemplate;

        public URI outgoingUriTemplate;

        public String outgoingUsernameTemplate;

        public String note;
    }
}
