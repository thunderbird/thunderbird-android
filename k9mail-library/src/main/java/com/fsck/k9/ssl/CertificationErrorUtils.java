package com.fsck.k9.ssl;

import android.net.Uri;
import android.util.Log;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.filter.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by philip on 11/03/2016.
 */
public class CertificationErrorUtils {
    // "hostname in certificate didn't match: <" + host + "> !=" + buf
    public static ArrayList<String> extractHostnames(String baseMessage) {
        LinkedHashSet<String> hostnames = new LinkedHashSet<String>();
        String remainingMessage = baseMessage;
        while(remainingMessage.length() > 1) {
            int startOfHostname = remainingMessage.indexOf('<');
            if(startOfHostname != -1) {
                int endOfHostname = remainingMessage.indexOf('>');
                if(endOfHostname != -1) {
                    hostnames.add(remainingMessage.substring(startOfHostname+1, endOfHostname));
                    remainingMessage = remainingMessage.substring(endOfHostname+1);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return new ArrayList<String>(hostnames);
    }

    public static String buildChainInfo(X509Certificate[] chain, String storeUri, String transportUri) {
        StringBuilder chainInfo = new StringBuilder(100);
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Log.e(K9MailLib.LOG_TAG, "Error while initializing MessageDigest", e);
        }

        for (int i = 0; i < chain.length; i++) {
            chainInfo.append(buildChainEntry(sha1, chain, i, storeUri, transportUri));
        }
        return chainInfo.toString();
    }

    private static StringBuilder buildChainEntry(MessageDigest sha1, X509Certificate[] chain,
                                                 int i, String storeUri, String transportUri) {
        // display certificate chain information
        //TODO: localize this strings
        StringBuilder chainEntry = new StringBuilder(100);

        chainEntry.append("Certificate chain[").append(i).append("]:\n");
        chainEntry.append("Subject: ").append(chain[i].getSubjectDN().toString()).append("\n");

        // display SubjectAltNames too
        // (the user may be mislead into mistrusting a certificate
        //  by a subjectDN not matching the server even though a
        //  SubjectAltName matches)
        try {
            final Collection<List<?>> subjectAlternativeNames = chain[i].getSubjectAlternativeNames();
            if (subjectAlternativeNames != null) {
                // The list of SubjectAltNames may be very long
                //TODO: localize this string
                StringBuilder altNamesText = new StringBuilder();
                altNamesText.append("Subject has ").append(subjectAlternativeNames.size()).append(" alternative names\n");

                // we need these for matching
                String storeURIHost = (Uri.parse(storeUri)).getHost();
                String transportURIHost = (Uri.parse(transportUri)).getHost();

                for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                    Integer type = (Integer) subjectAlternativeName.get(0);
                    Object value = subjectAlternativeName.get(1);
                    String name;
                    switch (type.intValue()) {
                        case 0:
                            Log.w(K9MailLib.LOG_TAG, "SubjectAltName of type OtherName not supported.");
                            continue;
                        case 1: // RFC822Name
                            name = (String) value;
                            break;
                        case 2:  // DNSName
                            name = (String) value;
                            break;
                        case 3:
                            Log.w(K9MailLib.LOG_TAG, "unsupported SubjectAltName of type x400Address");
                            continue;
                        case 4:
                            Log.w(K9MailLib.LOG_TAG, "unsupported SubjectAltName of type directoryName");
                            continue;
                        case 5:
                            Log.w(K9MailLib.LOG_TAG, "unsupported SubjectAltName of type ediPartyName");
                            continue;
                        case 6:  // Uri
                            name = (String) value;
                            break;
                        case 7: // ip-address
                            name = (String) value;
                            break;
                        default:
                            Log.w(K9MailLib.LOG_TAG, "unsupported SubjectAltName of unknown type");
                            continue;
                    }

                    // if some of the SubjectAltNames match the store or transport -host,
                    // display them
                    if (name.equalsIgnoreCase(storeURIHost) || name.equalsIgnoreCase(transportURIHost)) {
                        //TODO: localize this string
                        altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                    } else if (name.startsWith("*.") && (
                            storeURIHost.endsWith(name.substring(2)) ||
                                    transportURIHost.endsWith(name.substring(2)))) {
                        //TODO: localize this string
                        altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                    }
                }
                chainEntry.append(altNamesText);
            }
        } catch (Exception e1) {
            // don't fail just because of subjectAltNames
            Log.w(K9MailLib.LOG_TAG, "cannot display SubjectAltNames in dialog", e1);
        }

        chainEntry.append("Issuer: ").append(chain[i].getIssuerDN().toString()).append("\n");
        if (sha1 != null) {
            sha1.reset();
            try {
                char[] sha1sum = Hex.encodeHex(sha1.digest(chain[i].getEncoded()));
                chainEntry.append("Fingerprint (SHA-1): ").append(new String(sha1sum)).append("\n");
            } catch (CertificateEncodingException e) {
                Log.e(K9MailLib.LOG_TAG, "Error while encoding certificate", e);
            }
        }
        return chainEntry;
    }
}
