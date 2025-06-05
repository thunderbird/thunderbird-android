
package com.fsck.k9.mail.internet;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import net.thunderbird.core.logging.legacy.Log;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.util.MimeUtil;


public class MimeUtility {
    public static String unfold(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\r|\n", "");
    }

    private static String decode(String s, Message message) {
        if (s == null) {
            return null;
        } else {
            return DecoderUtil.decodeEncodedWords(s, message);
        }
    }

    public static String unfoldAndDecode(String s) {
        return unfoldAndDecode(s, null);
    }

    public static String unfoldAndDecode(String s, Message message) {
        return decode(unfold(s), message);
    }

    /**
     * Returns the named parameter of a header field.
     *
     * <p>
     * If name is {@code null} the "value" of the header is returned, i.e. "text/html" in the following example:
     * <br>
     *{@code Content-Type: text/html; charset="utf-8"}
     * </p>
     * <p>
     * Note: Parsing header parameters is not a very cheap operation. Prefer using {@code MimeParameterDecoder}
     * directly over calling this method multiple times for extracting different parameters from the same header.
     * </p>
     *
     * @param headerBody The header body.
     * @param parameterName The parameter name. Might be {@code null}.
     * @return the (parameter) value. if the parameter cannot be found the method returns null.
     */
    public static String getHeaderParameter(String headerBody, String parameterName) {
        if (headerBody == null) {
            return null;
        }

        if (parameterName == null) {
            return MimeParameterDecoder.extractHeaderValue(headerBody);
        } else {
            MimeValue mimeValue = MimeParameterDecoder.decode(headerBody);
            return mimeValue.getParameters().get(parameterName.toLowerCase(Locale.ROOT));
        }
    }

    public static Map<String,String> getAllHeaderParameters(String headerValue) {
        Map<String,String> result = new HashMap<>();

        headerValue = headerValue.replaceAll("\r|\n", "");
        String[] parts = headerValue.split(";");
        for (String part : parts) {
            String[] partParts = part.split("=", 2);
            if (partParts.length == 2) {
                String parameterName = partParts[0].trim().toLowerCase(Locale.US);
                String parameterValue = partParts[1].trim();
                result.put(parameterName, parameterValue);
            }
        }
        return result;
    }


    public static Part findFirstPartByMimeType(Part part, String mimeType) {
        if (part.getBody() instanceof Multipart) {
            Multipart multipart = (Multipart)part.getBody();
            for (BodyPart bodyPart : multipart.getBodyParts()) {
                Part ret = MimeUtility.findFirstPartByMimeType(bodyPart, mimeType);
                if (ret != null) {
                    return ret;
                }
            }
        } else if (isSameMimeType(part.getMimeType(), mimeType)) {
            return part;
        }
        return null;
    }

    /**
     * Returns true if the given mimeType matches the matchAgainst specification.
     * @param mimeType A MIME type to check.
     * @param matchAgainst A MIME type to check against. May include wildcards such as image/* or
     * * /*.
     * @return
     */
    public static boolean mimeTypeMatches(String mimeType, String matchAgainst) {
        Pattern p = Pattern.compile(matchAgainst.replaceAll("\\*", "\\.\\*"), Pattern.CASE_INSENSITIVE);
        return p.matcher(mimeType).matches();
    }

    /**
     * Get decoded contents of a body.
     * <p/>
     * Right now only some classes retain the original encoding of the body contents. Those classes have to implement
     * the {@link RawDataBody} interface in order for this method to decode the data delivered by
     * {@link Body#getInputStream()}.
     * <p/>
     * The ultimate goal is to get to a point where all classes retain the original data and {@code RawDataBody} can be
     * merged into {@link Body}.
     */
    public static InputStream decodeBody(Body body) throws MessagingException {
        InputStream inputStream;
        if (body instanceof RawDataBody) {
            RawDataBody rawDataBody = (RawDataBody) body;
            String encoding = rawDataBody.getEncoding();
            final InputStream rawInputStream = rawDataBody.getInputStream();
            if (MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding) || MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)
                    || MimeUtil.ENC_BINARY.equalsIgnoreCase(encoding)) {
                inputStream = rawInputStream;
            } else if (MimeUtil.ENC_BASE64.equalsIgnoreCase(encoding)) {
                inputStream = new Base64InputStream(rawInputStream, false) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        closeInputStreamWithoutDeletingTemporaryFiles(rawInputStream);
                    }
                };
            } else if (MimeUtil.ENC_QUOTED_PRINTABLE.equalsIgnoreCase(encoding)) {
                inputStream = new QuotedPrintableInputStream(rawInputStream) {
                    @Override
                    public void close() {
                        super.close();
                        try {
                            closeInputStreamWithoutDeletingTemporaryFiles(rawInputStream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            } else {
                Log.w("Unsupported encoding: %s", encoding);
                inputStream = rawInputStream;
            }
        } else {
            inputStream = body.getInputStream();
        }

        return inputStream;
    }

    public static void closeInputStreamWithoutDeletingTemporaryFiles(InputStream rawInputStream) throws IOException {
        if (rawInputStream instanceof BinaryTempFileBody.BinaryTempFileBodyInputStream) {
            ((BinaryTempFileBody.BinaryTempFileBodyInputStream) rawInputStream).closeWithoutDeleting();
        } else {
            rawInputStream.close();
        }
    }

    /**
     * Get a default content-transfer-encoding for use with a given content-type
     * when adding an unencoded attachment. It's possible that 8bit encodings
     * may later be converted to 7bit for 7bit transport.
     * <ul>
     * <li>null: base64
     * <li>message/rfc822: 8bit
     * <li>message/*: 7bit
     * <li>multipart/signed: 7bit
     * <li>multipart/*: 8bit
     * <li>*&#47;*: base64
     * </ul>
     *
     * @param type
     *            A String representing a MIME content-type
     * @return A String representing a MIME content-transfer-encoding
     */
    public static String getEncodingforType(String type) {
        if (type == null) {
            return (MimeUtil.ENC_BASE64);
        } else if (MimeUtil.isMessage(type)) {
            return (MimeUtil.ENC_8BIT);
        } else if (isSameMimeType(type, "multipart/signed") || isMessage(type)) {
            return (MimeUtil.ENC_7BIT);
        } else if (isMultipart(type)) {
            return (MimeUtil.ENC_8BIT);
        } else {
            return (MimeUtil.ENC_BASE64);
        }
    }

    public static boolean isMultipart(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("multipart/");
    }

    public static boolean isMessage(String mimeType) {
        return isSameMimeType(mimeType, "message/rfc822");
    }

    public static boolean isMessageType(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("message/");
    }

    public static boolean isSameMimeType(String mimeType, String otherMimeType) {
        return mimeType != null && mimeType.equalsIgnoreCase(otherMimeType);
    }
}
