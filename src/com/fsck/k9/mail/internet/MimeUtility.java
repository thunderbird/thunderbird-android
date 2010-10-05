
package com.fsck.k9.mail.internet;

import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.mail.*;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.decoder.Base64InputStream;
import org.apache.james.mime4j.decoder.DecoderUtil;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.nio.charset.Charset;


public class MimeUtility
{
    public static final String DEFAULT_ATTACHMENT_MIME_TYPE = "application/octet-stream";

    public static String unfold(String s)
    {
        if (s == null)
        {
            return null;
        }
        return s.replaceAll("\r|\n", "");
    }

    public static String decode(String s)
    {
        if (s == null)
        {
            return null;
        }
        return DecoderUtil.decodeEncodedWords(s);
    }

    public static String unfoldAndDecode(String s)
    {
        return decode(unfold(s));
    }

    // TODO implement proper foldAndEncode
    public static String foldAndEncode(String s)
    {
        return s;
    }

    /**
     * Returns the named parameter of a header field. If name is null the first
     * parameter is returned, or if there are no additional parameters in the
     * field the entire field is returned. Otherwise the named parameter is
     * searched for in a case insensitive fashion and returned. If the parameter
     * cannot be found the method returns null.
     *
     * @param header
     * @param name
     * @return
     */
    public static String getHeaderParameter(String header, String name)
    {
        if (header == null)
        {
            return null;
        }
        header = header.replaceAll("\r|\n", "");
        String[] parts = header.split(";");
        if (name == null)
        {
            return parts[0];
        }
        for (String part : parts)
        {
            if (part.trim().toLowerCase().startsWith(name.toLowerCase()))
            {
                String parameter = part.split("=", 2)[1].trim();
                if (parameter.startsWith("\"") && parameter.endsWith("\""))
                {
                    return parameter.substring(1, parameter.length() - 1);
                }
                else
                {
                    return parameter;
                }
            }
        }
        return null;
    }

    public static Part findFirstPartByMimeType(Part part, String mimeType)
    throws MessagingException
    {
        if (part.getBody() instanceof Multipart)
        {
            Multipart multipart = (Multipart)part.getBody();
            for (int i = 0, count = multipart.getCount(); i < count; i++)
            {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Part ret = findFirstPartByMimeType(bodyPart, mimeType);
                if (ret != null)
                {
                    return ret;
                }
            }
        }
        else if (part.getMimeType().equalsIgnoreCase(mimeType))
        {
            return part;
        }
        return null;
    }

    public static Part findPartByContentId(Part part, String contentId) throws Exception
    {
        if (part.getBody() instanceof Multipart)
        {
            Multipart multipart = (Multipart)part.getBody();
            for (int i = 0, count = multipart.getCount(); i < count; i++)
            {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Part ret = findPartByContentId(bodyPart, contentId);
                if (ret != null)
                {
                    return ret;
                }
            }
        }
        String[] header = part.getHeader("Content-ID");
        if (header != null)
        {
            for (String s : header)
            {
                if (s.equals(contentId))
                {
                    return part;
                }
            }
        }
        return null;
    }

    /**
      * Reads the Part's body and returns a String based on any charset conversion that needed
      * to be done.
      * @param part The part containing a body
      * @return a String containing the converted text in the body, or null if there was no text
      * or an error during conversion.
      */
    public static String getTextFromPart(Part part)
    {
        try
        {
            if ((part != null) && (part.getBody() != null))
            {
                final Body body = part.getBody();
                if (body instanceof TextBody)
                {
                    return ((TextBody)body).getText();
                }

                final String mimeType = part.getMimeType();
                if ((mimeType != null) && MimeUtility.mimeTypeMatches(mimeType, "text/*"))
                {
                    /*
                     * We've got a text part, so let's see if it needs to be processed further.
                     */
                    final String originalCharset = getHeaderParameter(part.getContentType(), "charset");
                    String charset = "ASCII";   // No encoding, so use us-ascii, which is the standard.
                    if ((originalCharset != null) && ("0".equals(originalCharset) == false))
                    {
                        /*
                         * See if there is conversion from the MIME charset to the Java one.
                         */

                        charset = Charset.forName(fixupCharset(originalCharset)).name();

                        if (charset == null)
                        {
                            Log.e(K9.LOG_TAG,"I don't know how to deal with the charset "+originalCharset+". Falling back to US-ASCII");
                            charset = "US-ASCII";
                        }
                    }

                    /*
                     * Now we read the part into a buffer for further processing. Because
                     * the stream is now wrapped we'll remove any transfer encoding at this point.
                     */
                    InputStream in = part.getBody().getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
                    IOUtils.copy(in, out);
                    in.close();
                    in = null;      // we want all of our memory back, and close might not release
                    // Cargo culted from AOSP - This disagrees with the platform docs

                    /*
                     * Convert and return as new String
                     */
                    final String result = out.toString(charset);
                    out.close();
                    return result;
                }
            }

        }
        catch (OutOfMemoryError oom)
        {
            /*
             * If we are not able to process the body there's nothing we can do about it. Return
             * null and let the upper layers handle the missing content.
             */
            Log.e(K9.LOG_TAG, "Unable to getTextFromPart " + oom.toString());
        }
        catch (Exception e)
        {
            /*
             * If we are not able to process the body there's nothing we can do about it. Return
             * null and let the upper layers handle the missing content.
             */
            Log.e(K9.LOG_TAG, "Unable to getTextFromPart", e);
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
    public static boolean mimeTypeMatches(String mimeType, String matchAgainst)
    {
        Pattern p = Pattern.compile(matchAgainst.replaceAll("\\*", "\\.\\*"),
                                    Pattern.CASE_INSENSITIVE);
        return p.matcher(mimeType).matches();
    }

    /**
     * Returns true if the given mimeType matches any of the matchAgainst specifications.
     * @param mimeType A MIME type to check.
     * @param matchAgainst An array of MIME types to check against. May include wildcards such
     * as image/* or * /*.
     * @return
     */
    public static boolean mimeTypeMatches(String mimeType, String[] matchAgainst)
    {
        for (String matchType : matchAgainst)
        {
            if (mimeTypeMatches(mimeType, matchType))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes any content transfer encoding from the stream and returns a Body.
     */
    public static Body decodeBody(InputStream in, String contentTransferEncoding)
    throws IOException
    {
        /*
         * We'll remove any transfer encoding by wrapping the stream.
         */
        if (contentTransferEncoding != null)
        {
            contentTransferEncoding =
                MimeUtility.getHeaderParameter(contentTransferEncoding, null);
            if ("quoted-printable".equalsIgnoreCase(contentTransferEncoding))
            {
                in = new QuotedPrintableInputStream(in);
            }
            else if ("base64".equalsIgnoreCase(contentTransferEncoding))
            {
                in = new Base64InputStream(in);
            }
        }

        BinaryTempFileBody tempBody = new BinaryTempFileBody();
        OutputStream out = tempBody.getOutputStream();
        IOUtils.copy(in, out);
        out.close();
        return tempBody;
    }

    /**
     * An unfortunately named method that makes decisions about a Part (usually a Message)
     * as to which of it's children will be "viewable" and which will be attachments.
     * The method recursively sorts the viewables and attachments into seperate
     * lists for further processing.
     * @param part
     * @param viewables
     * @param attachments
     * @throws MessagingException
     */
    public static void collectParts(Part part, ArrayList<Part> viewables,
                                    ArrayList<Part> attachments) throws MessagingException
    {
        /*
         * If the part is Multipart but not alternative it's either mixed or
         * something we don't know about, which means we treat it as mixed
         * per the spec. We just process it's pieces recursively.
         */
        if (part.getBody() instanceof Multipart)
        {
            Multipart mp = (Multipart)part.getBody();
            for (int i = 0; i < mp.getCount(); i++)
            {
                collectParts(mp.getBodyPart(i), viewables, attachments);
            }
        }
        /*
         * If the part is an embedded message we just continue to process
         * it, pulling any viewables or attachments into the running list.
         */
        else if (part.getBody() instanceof Message)
        {
            Message message = (Message)part.getBody();
            collectParts(message, viewables, attachments);
        }
        /*
         * If the part is HTML and it got this far it's part of a mixed (et
         * al) and should be rendered inline.
         */
        else if (isPartTextualBody(part))
        {
            viewables.add(part);
        }
        else
        {
            attachments.add(part);
        }

    }


    public static Boolean isPartTextualBody(Part part) throws MessagingException
    {
        String disposition = part.getDisposition();
        String dispositionType = null;
        String dispositionFilename = null;
        if (disposition != null)
        {
            dispositionType = MimeUtility.getHeaderParameter(disposition, null);
            dispositionFilename = MimeUtility.getHeaderParameter(disposition, "filename");
        }

        /*
         * A best guess that this part is intended to be an attachment and not inline.
         */
        boolean attachment = ("attachment".equalsIgnoreCase(dispositionType)
                              || (dispositionFilename != null));

        if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/html")))
        {
            return true;
        }
        /*
         * If the part is plain text and it got this far it's part of a
         * mixed (et al) and should be rendered inline.
         */
        else if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/plain")))
        {
            return true;
        }
        /*
         * Finally, if it's nothing else we will include it as an attachment.
         */
        else
        {
            return false;
        }
    }


    public static String getMimeTypeByExtension(String filename)
    {
        String returnedType = null;
        if (filename != null
                && filename.lastIndexOf('.') != -1)
        {
            String extension = filename.substring(filename.lastIndexOf('.')+1).toLowerCase();
            returnedType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (returnedType != null) {
            return returnedType;
        }
        else
        {
            return DEFAULT_ATTACHMENT_MIME_TYPE;
        }
    }


    private static String fixupCharset(String charset)
    {
        charset = charset.toLowerCase();
        if (charset.equals("cp932"))
            return "shift-jis";
        else if (charset.equals("koi8-u"))
            return "koi8-r";

        return charset;

    }


}
