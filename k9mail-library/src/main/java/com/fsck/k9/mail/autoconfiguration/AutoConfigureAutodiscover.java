package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;
import java.net.UnknownHostException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


/**
 * Microsoft Exchange's autodiscover
 * Not support redirectAddr
 */

public class AutoConfigureAutodiscover implements AutoConfigure {
    private final static String AUTODISCOVER_URL1 = "https://%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_URL2 = "https://autodiscover.%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_URL3 = "http://autodiscover.%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_SRV = "_autodiscover._tcp.%s";

    @Override
    public ProviderInfo findProviderInfo(String domain) {
        ProviderInfo providerInfo = null;

        String url = String.format(AUTODISCOVER_URL1, domain);
        providerInfo = findProviderInfoByUrl(url);

        if (providerInfo != null) return providerInfo;

        url = String.format(AUTODISCOVER_URL2, domain);
        providerInfo = findProviderInfoByUrl(url);

        if (providerInfo != null) return providerInfo;

        url = String.format(AUTODISCOVER_URL3, domain);
        providerInfo = findProviderInfoByUrl(url, true);

        url = String.format(AUTODISCOVER_SRV, domain);
        DNSOperation dnsOperation = new DNSOperation();
        try {
            SRVRecord srvRecord = dnsOperation.srvLookup(url);
            url = srvRecord.getTarget().toString(true);
            providerInfo = findProviderInfoByUrl(url);
        } catch (TextParseException e) {
            Timber.e(e, "Error while trying to do SRV lookup");
        } catch (UnknownHostException e) {
            Timber.w(e, "No valid SRV record for " + domain);
        }

        return providerInfo;
    }

    private ProviderInfo findProviderInfoByUrl(String url) {
        return findProviderInfoByUrl(url, false);
    }

    private ProviderInfo findProviderInfoByUrl(String url, boolean followRedirects) {
        ProviderInfo providerInfo = null;
        try {
            Document document = Jsoup.connect(url).followRedirects(followRedirects).get();
            Element account = document.select("Account").first();
            if (account == null) {
                return null;
            }
            Element accountType = account.select("AccountType").first();
            if (accountType == null || !accountType.text().equals("email")) {
                return null;
            }
            Element action = account.select("Action").first();
            if (action.text().equalsIgnoreCase("settings")) {
                providerInfo = parse(account);
            } else if (account.text().equalsIgnoreCase("redirectUrl")) {
                Element redirectUrl = account.select("RedirectUrl").first();
                if (redirectUrl != null) {
                    providerInfo = findProviderInfoByUrl(redirectUrl.text());
                }
            }

        } catch (IOException e) {
            Timber.w(e, "No information at " + url);
        }
        return providerInfo;
    }

    private ProviderInfo parse(Element account) {
        ProviderInfo providerInfo = new ProviderInfo();
        Elements protocols = account.select("Protocol");
        for (Element protocol : protocols) {
            Element type = protocol.select("Type").first();

            if ((type.text().equalsIgnoreCase("POP3") ||
                    type.text().equalsIgnoreCase("IMAP"))) {

                if (type.text().equalsIgnoreCase("POP3")) {
                    providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
                } else {
                    providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
                }

                Element server = protocol.select("Server").first();
                if (server == null) {
                    continue;
                }
                providerInfo.incomingAddr = server.text();

                Element port = protocol.select("Port").first();
                if (port != null) {
                    providerInfo.incomingPort = Integer.valueOf(port.text());
                }

                Element loginName = protocol.select("LoginName").first();
                if (loginName != null) {
                    providerInfo.incomingUsernameTemplate = loginName.text();
                } else {
                    providerInfo.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
                }

                Element domainRequired = protocol.select("DomainRequired").first();
                if (domainRequired != null && domainRequired.text().equalsIgnoreCase("on")) {
                    Element domainName = protocol.select("DomainName").first();
                    if (!providerInfo.incomingUsernameTemplate.isEmpty()) {
                        providerInfo.incomingUsernameTemplate += "@"
                                + ((domainName != null && !domainName.text().isEmpty()) ?
                                domainName.text() : ProviderInfo.USERNAME_TEMPLATE_DOMAIN);
                    } else {
                        providerInfo.incomingUsernameTemplate =
                                domainName != null && !domainName.text().isEmpty() ?
                                        domainName.text() : ProviderInfo.USERNAME_TEMPLATE_DOMAIN;
                    }
                }

                Element SSL = protocol.select("SSL").first();
                if (SSL != null && SSL.text().equalsIgnoreCase("on")) {
                    providerInfo.incomingSocketType = "ssl";
                }

                Element TLS = protocol.select("TLS").first();
                if (TLS != null && TLS.text().equalsIgnoreCase("on")) {
                    providerInfo.incomingSocketType = "tls";
                }

                Element encryption = protocol.select("Encryption").first();
                if (encryption != null) {
                    providerInfo.incomingSocketType = encryption.text();
                }
                break;
            }
        }

        for (Element protocol : protocols) {
            Element type = protocol.select("Type").first();

            if (type.text().equalsIgnoreCase("SMTP")) {
                providerInfo.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;

                Element usePopAuth = protocol.select("UsePOPAuth").first();
                if (usePopAuth != null && usePopAuth.text().equalsIgnoreCase("on")) {
                    providerInfo.outgoingAddr = providerInfo.incomingAddr;
                    providerInfo.outgoingSocketType = providerInfo.incomingSocketType;
                    providerInfo.outgoingUsernameTemplate = providerInfo.incomingUsernameTemplate;
                }

                Element server = protocol.select("Server").first();
                if (server == null) {
                    continue;
                }
                providerInfo.outgoingAddr = server.text();

                Element port = protocol.select("Port").first();
                if (port != null) {
                    providerInfo.outgoingPort = Integer.valueOf(port.text());
                }

                Element loginName = protocol.select("LoginName").first();
                if (loginName != null) {
                    providerInfo.outgoingUsernameTemplate = loginName.text();
                } else {
                    providerInfo.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
                }

                Element domainRequired = protocol.select("DomainRequired").first();
                if (domainRequired != null && domainRequired.text().equalsIgnoreCase("on")) {
                    Element domainName = protocol.select("DomainName").first();
                    if (!providerInfo.outgoingUsernameTemplate.isEmpty()) {
                        providerInfo.outgoingUsernameTemplate += "@"
                                + ((domainName != null && !domainName.text().isEmpty()) ?
                                domainName.text() : ProviderInfo.USERNAME_TEMPLATE_DOMAIN);
                    } else {
                        providerInfo.outgoingUsernameTemplate =
                                domainName != null && !domainName.text().isEmpty() ?
                                        domainName.text() : ProviderInfo.USERNAME_TEMPLATE_DOMAIN;
                    }
                }

                Element SSL = protocol.select("SSL").first();
                if (SSL != null && SSL.text().equalsIgnoreCase("on") &&
                        providerInfo.outgoingSocketType.isEmpty()) {
                    providerInfo.outgoingSocketType = "ssl";
                }

                Element TLS = protocol.select("TLS").first();
                if (TLS != null && TLS.text().equalsIgnoreCase("on") &&
                        providerInfo.outgoingSocketType.isEmpty()) {
                    providerInfo.outgoingSocketType = "tls";
                }

                Element encryption = protocol.select("Encryption").first();
                if (encryption != null && providerInfo.outgoingSocketType.isEmpty()) {
                    providerInfo.outgoingSocketType = encryption.text();
                }
            }
        }

        return providerInfo;
    }
}
