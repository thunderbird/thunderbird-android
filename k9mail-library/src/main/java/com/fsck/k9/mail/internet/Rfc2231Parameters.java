package com.fsck.k9.mail.internet;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.Message;


public class Rfc2231Parameters {
    /**
     * Decodes a string containing encoded words as defined by RFC 2231.
     *
     * @param body
     *         the string to decode.
     * @param message
     *         the message which has the string.
     *
     * @return the decoded string.
     */
    public static String decodeAll(@NonNull String body, Message message) {
        String body2 = body.replaceAll("[\r\n]", "");
        String[] lines = body2.split("[\t ]*;[\t ]*");
        Rfc2231Parameters params = new Rfc2231Parameters(message);
        for (String line : lines) {
            params.addLine(line);
        }
        return params.getDecodedText();
    }

    private static final int SINGLE_LINE_INDEX = -1;
    private Message message;

    private Map<String, SortedMap<Integer, Rfc2231Value>> parameters = new LinkedHashMap<>();

    private void addParameter(String attribute) {
        parameters.put(attribute, null);
    }

    private void addParameter(String attribute, Integer index, String value, boolean isEncoded) {
        if (!parameters.containsKey(attribute)) {
            parameters.put(attribute, new TreeMap<Integer, Rfc2231Value>());
        }
        parameters.get(attribute).put(index, new Rfc2231Value(value, isEncoded));
    }

    public Rfc2231Parameters(Message message) {
        this.message = message;
    }

    public void addLine(String line) {
        String[] attrAndValue = splitAttributeAndValue(line);
        mapParameter(attrAndValue);
    }

    public String getDecodedText() {
        StringBuilder sb = new StringBuilder();
        String prefix = "";

        for (Entry<String, SortedMap<Integer, Rfc2231Value>> parameter : parameters.entrySet()) {
            SortedMap<Integer, Rfc2231Value> lines = parameter.getValue();

            if (lines == null) {
                sb.append(prefix).append(parameter.getKey());
                prefix = ";";
                continue;
            }

            Rfc2231Decoder decoder = new Rfc2231Decoder(message);

            if (lines.size() == 1) {
                decoder.addLine(lines.get(lines.firstKey()));
            } else {
                for (Entry<Integer, Rfc2231Value> line : lines.entrySet()) {
                    if (line.getKey() < 0) {
                        continue;
                    }
                    decoder.addLine(line.getValue());
                }
            }

            sb.append(prefix).append(parameter.getKey()).append("=").append(decoder.getDecodedText());
            prefix = ";";
        }

        return sb.toString();
    }

    private String[] splitAttributeAndValue(String line) {
        String[] parts = line.split("[¥t ]*=[¥t ]*", 2);
        if (parts.length == 2) {
            parts[1] = deQuote(parts[1]);
        }
        return parts;
    }

    private String deQuote(String source) {
        int len = source.length();
        if (len < 2) {
            return source;
        }

        if (source.startsWith("\\\"") && source.endsWith("\\\"")) {
            return source.substring(1, len - 1);
        } else {
            return source;
        }
    }

    private void mapParameter(String[] parameter) {
        final Pattern pattern = Pattern.compile("^(.+)\\*([0-9]+)(\\*?)$");
        // { "name*0", "name*1", ... } and { "name*0*", "name*1*", ... }

        // no "attr=value" text, example: "Content-Disposition: attachment"
        if (parameter.length < 2) {
            addParameter(parameter[0]);
            return;
        }

        Matcher m = pattern.matcher(parameter[0]);
        if (m.find()) { // matches only once (whole string), so "while" is needless.
            addParameter(
                    m.group(1),
                    Integer.parseInt(m.group(2)),
                    parameter[1],
                    m.group(3).equals("*"));
        } else {
            addParameter(
                    parameter[0],
                    SINGLE_LINE_INDEX,
                    parameter[1],
                    parameter[0].endsWith("*"));
        }
    }
}
