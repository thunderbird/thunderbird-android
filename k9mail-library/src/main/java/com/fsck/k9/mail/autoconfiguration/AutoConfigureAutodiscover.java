package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;
import java.net.UnknownHostException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import timber.log.Timber;


/**
 * "Autodiscover" is Microsoft Exchange's autoconfiguration mechanism
 * Not support redirectAddr
 */

public class AutoConfigureAutodiscover implements AutoConfigure {
    private final static String AUTODISCOVER_URL1 = "https://%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_URL2 = "https://autodiscover.%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_URL3 = "http://autodiscover.%s/autodiscover/autodiscover.xml";
    private final static String AUTODISCOVER_SRV = "_autodiscover._tcp.%s";

    private final static String AUTODISCOVER_POST_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/requestschema/2006\">\n" +
            "<Request>\n" +
            "<AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a</AcceptableResponseSchema>\n" +
            "\n" +
            "<EMailAddress>%s</EMailAddress>\n" +
            "</Request>\n" +
            "</Autodiscover>";

    @Override
    public ProviderInfo findProviderInfo(ProviderInfo providerInfo, String localpart, String domain) {
        String email = localpart + "@" + domain;

        String url = String.format(AUTODISCOVER_URL1, domain);
        providerInfo = findProviderInfoByUrl(providerInfo, url, email);

        if (providerInfo != null) return providerInfo;

        url = String.format(AUTODISCOVER_URL2, domain);
        providerInfo = findProviderInfoByUrl(providerInfo, url, email);

        if (providerInfo != null) return providerInfo;

        url = String.format(AUTODISCOVER_URL3, domain);
        providerInfo = findProviderInfoByUrl(providerInfo, url, email, true);

        url = String.format(AUTODISCOVER_SRV, domain);
        DnsOperation dnsOperation = new DnsOperation();
        try {
            SRVRecord srvRecord = dnsOperation.choose(dnsOperation.srvLookup(url));
            if (srvRecord != null) {
                url = srvRecord.getTarget().toString(true);
                providerInfo = findProviderInfoByUrl(providerInfo, url, email);
            }
        } catch (TextParseException e) {
            Timber.e(e, "Error while trying to do SRV lookup");
        } catch (UnknownHostException e) {
            Timber.w(e, "No valid SRV record for " + domain);
        }

        return providerInfo;
    }

    private ProviderInfo findProviderInfoByUrl(ProviderInfo providerInfo, String url, String email) {
        return findProviderInfoByUrl(providerInfo, url, email, false);
    }

    private ProviderInfo findProviderInfoByUrl(ProviderInfo providerInfo, String url, String email, boolean followRedirects) {
        try {
            Document document = Jsoup.connect(url).timeout(5000).requestBody(String.format(AUTODISCOVER_POST_BODY, email))
                    .followRedirects(followRedirects).post();
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
                providerInfo = parse(providerInfo, account);
            } else if (account.text().equalsIgnoreCase("redirectUrl")) {
                Element redirectUrl = account.select("RedirectUrl").first();
                if (redirectUrl != null) {
                    providerInfo = findProviderInfoByUrl(providerInfo, redirectUrl.text(), email);
                }
            }

        } catch (IOException e) {
            Timber.w(e, "No information at " + url);
        }
        return providerInfo;
    }

    public ProviderInfo parse(ProviderInfo providerInfo, Element account) {
        /*
        Elements protocols = account.select("Protocol");
        for (Element protocol : protocols) {
            Element type = protocol.select("Type").first();

            if ((type.text().equalsIgnoreCase("POP3") ||
                    type.text().equalsIgnoreCase("IMAP"))) {

                Element server = protocol.select("Server").first();
                if (server == null) {
                    continue;
                }

                if (type.text().equalsIgnoreCase("POP3")) {
                    providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
                } else {
                    providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
                }


                providerInfo.incomingHost = server.text();

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
                    providerInfo.incomingSecurity = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
                } else {
                    providerInfo.incomingSecurity = ProviderInfo.SOCKET_TYPE_STARTTLS;
                }

                Element TLS = protocol.select("TLS").first();
                if (TLS != null && TLS.text().equalsIgnoreCase("on")) {
                    providerInfo.incomingSecurity = ProviderInfo.SOCKET_TYPE_STARTTLS;
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
                    providerInfo.outgoingHost = providerInfo.incomingHost;
                    providerInfo.outgoingSecurity = providerInfo.incomingSecurity;
                    providerInfo.outgoingUsernameTemplate = providerInfo.incomingUsernameTemplate;
                }

                Element server = protocol.select("Server").first();
                if (server == null && providerInfo.outgoingHost.equals("")) {
                    providerInfo.outgoingUsernameTemplate = "";
                    providerInfo.outgoingHost = "";
                    providerInfo.outgoingSecurity = "";
                    providerInfo.outgoingPort = -1;

                    continue;
                }
                if (server != null) {
                    providerInfo.outgoingHost = server.text();
                }

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
                        providerInfo.outgoingSecurity.isEmpty()) {
                    providerInfo.outgoingSecurity = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
                }

                Element TLS = protocol.select("TLS").first();
                if (TLS != null && TLS.text().equalsIgnoreCase("on") &&
                        providerInfo.outgoingSecurity.isEmpty()) {
                    providerInfo.outgoingSecurity = ProviderInfo.SOCKET_TYPE_STARTTLS;
                }

                Element encryption = protocol.select("Encryption").first();
                if (encryption != null && providerInfo.outgoingSecurity.isEmpty()) {
                    providerInfo.outgoingSecurity = encryption.text();
                }
            }
        }

        if (providerInfo.incomingHost.equals("") || providerInfo.outgoingHost.equals("")) {
            return null;
        }
        */

        return providerInfo;
    }
}
