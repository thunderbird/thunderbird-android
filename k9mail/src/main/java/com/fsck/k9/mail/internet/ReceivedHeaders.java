package com.fsck.k9.mail.internet;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes ReceivedHeaders to extract information
 */
public class ReceivedHeaders {
    public static final String RECEIVED = "Received";
    public static Pattern fromPattern = Pattern.compile("from ([A-Za-z0-9.]*?) ");
    public static Pattern byPattern = Pattern.compile("by ([A-Za-z0-9.]*?) ");
    public static Pattern usingPattern = Pattern.compile("using (.*?)");
    public static Pattern cipherPattern = Pattern.compile("cipher (.*?)");
    public static Pattern bitsPattern = Pattern.compile("([0-9]*?)/([0-9]*?) bits");

    public static SecureTransportState wasMessageTransmittedSecurely(Message message) {
        try {
            String[] headers = message.getHeader(RECEIVED);
            Log.e(K9.LOG_TAG, "Received headers: " + headers.length);

            for (String header: headers) {
                String fromAddress = "", toAddress = "", sslVersion = null, cipher, bits1, bits2;
                header = header.trim();

                Matcher matcher = fromPattern.matcher(header);
                if (matcher.find())
                    fromAddress = matcher.group(1);

                matcher = byPattern.matcher(header);
                if (matcher.find())
                    toAddress = matcher.group(1);

                matcher = usingPattern.matcher(header);
                if (matcher.find()) {
                    sslVersion = matcher.group(1);
                    matcher = cipherPattern.matcher(header);
                    if (matcher.find())
                        cipher = matcher.group(1);
                    matcher = bitsPattern.matcher(header);
                    if (matcher.find())
                        bits1 = matcher.group(1);
                        bits1 = matcher.group(2);
                }

                if (fromAddress.equals("localhost") || fromAddress.equals("127.0.0.1") ||
                        toAddress.equals("localhost") || toAddress.equals("127.0.0.1")) {
                    //Loopback is considered secure
                    continue;
                }

                if (sslVersion == null || sslVersion.startsWith("SSL")) {
                    //SSLv1, v2, v3 considered broken
                    return SecureTransportState.INSECURE;
                }

                //TODO: Blacklisted ciphers and key lengths

                return SecureTransportState.SECURE;
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to fetch message headers:", e);
        }
        return SecureTransportState.UNKNOWN;
    }
}
