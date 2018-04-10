package com.fsck.k9.mail.internet;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;


public class Rfc2231Decoder {
    private String charset;
    private String language;

    public String getLanguage() {
        return language;
    }

    private List<Byte> bytes;
    private String text;
    private Message message;

    Rfc2231Decoder(Message message) {
        this.message = message;
    }

    public void addLine(@NonNull Rfc2231Value value) {
        // first value
        if (charset == null) {
            if (value.isEncoded()) {
                if (analyzeFirstValue(value)) {
                    return;
                }
            }
            charset = "";
            text = value.getString();
            return;
        }

        // continued value
        text += value.getString();
        if (!charset.equals("")) {
            decode(value.getString());
        }
    }

    public String getDecodedText() {
        if (charset == null) {
            return null;
        }
        if (charset.equals("")) {
            return text;
        }

        int len = bytes.size();
        byte[] byteArray = new byte[len];
        for (int i = 0; i < len; ++i) {
            byteArray[i] = bytes.get(i);
        }
        try {
            return new String(byteArray, Charset.forName(charset));
        } catch (Exception ex) {
            return text;
        }
    }

    private boolean analyzeFirstValue(@NonNull Rfc2231Value value) {
        final String DELIMITER = "'";

        String str = value.getString();
        int iDelimiter1 = str.indexOf(DELIMITER);
        if (iDelimiter1 < 0) {
            return false;
        }
        int iDelimiter2 = str.indexOf(DELIMITER, iDelimiter1 + 1);
        if (iDelimiter2 < 0) {
            return false;
        }

        String charsetRaw = str.substring(0, iDelimiter1);
        try {
            charset = CharsetSupport.fixupCharset(charsetRaw, message);
        } catch (MessagingException e) {
            charset = "";
        }

        language = str.substring(iDelimiter1 + 1, iDelimiter2);
        text = str.substring(iDelimiter2 + 1);
        bytes = new ArrayList<>();
        decode(text);

        return true;
    }

    private void decode(@NonNull String str) {
        final Pattern pattern = Pattern.compile("%([0-9A-Fa-f]{2})");
        Matcher m = pattern.matcher(str);
        int i = 0;

        while (m.find()) {
            addBytes(str, i, m.start());
            addByte(m.group(1));
            i = m.end();
        }

        int len = str.length();
        if (i < len) {
            addBytes(str, i, len);
        }
    }

    private void addBytes(@NonNull String str, int beginIndex, int endIndex) {
        for (byte b : str.substring(beginIndex, endIndex).getBytes(Charset.forName("US-ASCII"))) {
            bytes.add(b);
        }
    }

    private void addByte(@NonNull String hexText) {
        int i = Integer.parseInt(hexText, 16);
        byte b = (byte) i;
        bytes.add(b);
    }
}
