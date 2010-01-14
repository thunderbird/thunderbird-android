
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MimeUtility
{
    public static final String DEFAULT_ATTACHMENT_MIME_TYPE = "application/octet-stream";

    /*
     * http://www.w3schools.com/media/media_mimeref.asp
     * + png
     */
    public static final String[][] MIME_TYPE_BY_EXTENSION_MAP = new String[][]
    {
        { "",       "application/octet-stream" },
        { "323",    "text/h323"},
        { "acx",    "application/internet-property-stream"},
        { "ai",     "application/postscript"},
        { "aif",    "audio/x-aiff"},
        { "aifc",   "audio/x-aiff"},
        { "aiff",   "audio/x-aiff"},
        { "asf",    "video/x-ms-asf"},
        { "asr",    "video/x-ms-asf"},
        { "asx",    "video/x-ms-asf"},
        { "au",     "audio/basic"},
        { "avi",    "video/x-msvideo"},
        { "axs",    "application/olescript"},
        { "bas",    "text/plain"},
        { "bcpio",  "application/x-bcpio"},
        { "bin",    "application/octet-stream"},
        { "bmp",    "image/bmp"},
        { "c",      "text/plain"},
        { "cat",    "application/vnd.ms-pkiseccat"},
        { "cdf",    "application/x-cdf"},
        { "cer",    "application/x-x509-ca-cert"},
        { "class",  "application/octet-stream"},
        { "clp",    "application/x-msclip"},
        { "cmx",    "image/x-cmx"},
        { "cod",    "image/cis-cod"},
        { "cpio",   "application/x-cpio"},
        { "crd",    "application/x-mscardfile"},
        { "crl",    "application/pkix-crl"},
        { "crt",    "application/x-x509-ca-cert"},
        { "csh",    "application/x-csh"},
        { "css",    "text/css"},
        { "dcr",    "application/x-director"},
        { "der",    "application/x-x509-ca-cert"},
        { "dir",    "application/x-director"},
        { "dll",    "application/x-msdownload"},
        { "dms",    "application/octet-stream"},
        { "doc",    "application/msword"},
        { "dot",    "application/msword"},
        { "dvi",    "application/x-dvi"},
        { "dxr",    "application/x-director"},
        { "eps",    "application/postscript"},
        { "etx",    "text/x-setext"},
        { "evy",    "application/envoy"},
        { "exe",    "application/octet-stream"},
        { "fif",    "application/fractals"},
        { "flr",    "x-world/x-vrml"},
        { "gif",    "image/gif"},
        { "gtar",   "application/x-gtar"},
        { "gz",     "application/x-gzip"},
        { "h",      "text/plain"},
        { "hdf",    "application/x-hdf"},
        { "hlp",    "application/winhlp"},
        { "hqx",    "application/mac-binhex40"},
        { "hta",    "application/hta"},
        { "htc",    "text/x-component"},
        { "htm",    "text/html"},
        { "html",   "text/html"},
        { "htt",    "text/webviewhtml"},
        { "ico",    "image/x-icon"},
        { "ief",    "image/ief"},
        { "iii",    "application/x-iphone"},
        { "ins",    "application/x-internet-signup"},
        { "isp",    "application/x-internet-signup"},
        { "jfif",   "image/pipeg"},
        { "jpe",    "image/jpeg"},
        { "jpeg",   "image/jpeg"},
        { "jpg",    "image/jpeg"},
        { "js",     "application/x-javascript"},
        { "latex",  "application/x-latex"},
        { "lha",    "application/octet-stream"},
        { "lsf",    "video/x-la-asf"},
        { "lsx",    "video/x-la-asf"},
        { "lzh",    "application/octet-stream"},
        { "m13",    "application/x-msmediaview"},
        { "m14",    "application/x-msmediaview"},
        { "m3u",    "audio/x-mpegurl"},
        { "man",    "application/x-troff-man"},
        { "mdb",    "application/x-msaccess"},
        { "me",     "application/x-troff-me"},
        { "mht",    "message/rfc822"},
        { "mhtml",  "message/rfc822"},
        { "mid",    "audio/mid"},
        { "mny",    "application/x-msmoney"},
        { "mov",    "video/quicktime"},
        { "movie",  "video/x-sgi-movie"},
        { "mp2",    "video/mpeg"},
        { "mp3",    "audio/mpeg"},
        { "mpa",    "video/mpeg"},
        { "mpe",    "video/mpeg"},
        { "mpeg",   "video/mpeg"},
        { "mpg",    "video/mpeg"},
        { "mpp",    "application/vnd.ms-project"},
        { "mpv2",   "video/mpeg"},
        { "ms",     "application/x-troff-ms"},
        { "mvb",    "application/x-msmediaview"},
        { "nws",    "message/rfc822"},
        { "oda",    "application/oda"},
        { "p10",    "application/pkcs10"},
        { "p12",    "application/x-pkcs12"},
        { "p7b",    "application/x-pkcs7-certificates"},
        { "p7c",    "application/x-pkcs7-mime"},
        { "p7m",    "application/x-pkcs7-mime"},
        { "p7r",    "application/x-pkcs7-certreqresp"},
        { "p7s",    "application/x-pkcs7-signature"},
        { "pbm",    "image/x-portable-bitmap"},
        { "pdf",    "application/pdf"},
        { "pfx",    "application/x-pkcs12"},
        { "pgm",    "image/x-portable-graymap"},
        { "pko",    "application/ynd.ms-pkipko"},
        { "pma",    "application/x-perfmon"},
        { "pmc",    "application/x-perfmon"},
        { "pml",    "application/x-perfmon"},
        { "pmr",    "application/x-perfmon"},
        { "pmw",    "application/x-perfmon"},
        { "png",    "image/png"},
        { "pnm",    "image/x-portable-anymap"},
        { "pot,",   "application/vnd.ms-powerpoint"},
        { "ppm",    "image/x-portable-pixmap"},
        { "pps",    "application/vnd.ms-powerpoint"},
        { "ppt",    "application/vnd.ms-powerpoint"},
        { "prf",    "application/pics-rules"},
        { "ps",     "application/postscript"},
        { "pub",    "application/x-mspublisher"},
        { "qt",     "video/quicktime"},
        { "ra",     "audio/x-pn-realaudio"},
        { "ram",    "audio/x-pn-realaudio"},
        { "ras",    "image/x-cmu-raster"},
        { "rgb",    "image/x-rgb"},
        { "rmi",    "audio/mid"},
        { "roff",   "application/x-troff"},
        { "rtf",    "application/rtf"},
        { "rtx",    "text/richtext"},
        { "scd",    "application/x-msschedule"},
        { "sct",    "text/scriptlet"},
        { "setpay", "application/set-payment-initiation"},
        { "setreg", "application/set-registration-initiation"},
        { "sh",     "application/x-sh"},
        { "shar",   "application/x-shar"},
        { "sit",    "application/x-stuffit"},
        { "snd",    "audio/basic"},
        { "spc",    "application/x-pkcs7-certificates"},
        { "spl",    "application/futuresplash"},
        { "src",    "application/x-wais-source"},
        { "sst",    "application/vnd.ms-pkicertstore"},
        { "stl",    "application/vnd.ms-pkistl"},
        { "stm",    "text/html"},
        { "svg",    "image/svg+xml"},
        { "sv4cpio","application/x-sv4cpio"},
        { "sv4crc", "application/x-sv4crc"},
        { "swf",    "application/x-shockwave-flash"},
        { "t",      "application/x-troff"},
        { "tar",    "application/x-tar"},
        { "tcl",    "application/x-tcl"},
        { "tex",    "application/x-tex"},
        { "texi",   "application/x-texinfo"},
        { "texinfo","application/x-texinfo"},
        { "tgz",    "application/x-compressed"},
        { "tif",    "image/tiff"},
        { "tiff",   "image/tiff"},
        { "tr",     "application/x-troff"},
        { "trm",    "application/x-msterminal"},
        { "tsv",    "text/tab-separated-values"},
        { "txt",    "text/plain"},
        { "uls",    "text/iuls"},
        { "ustar",  "application/x-ustar"},
        { "vcf",    "text/x-vcard"},
        { "vrml",   "x-world/x-vrml"},
        { "wav",    "audio/x-wav"},
        { "wcm",    "application/vnd.ms-works"},
        { "wdb",    "application/vnd.ms-works"},
        { "wks",    "application/vnd.ms-works"},
        { "wmf",    "application/x-msmetafile"},
        { "wps",    "application/vnd.ms-works"},
        { "wri",    "application/x-mswrite"},
        { "wrl",    "x-world/x-vrml"},
        { "wrz",    "x-world/x-vrml"},
        { "xaf",    "x-world/x-vrml"},
        { "xbm",    "image/x-xbitmap"},
        { "xla",    "application/vnd.ms-excel"},
        { "xlc",    "application/vnd.ms-excel"},
        { "xlm",    "application/vnd.ms-excel"},
        { "xls",    "application/vnd.ms-excel"},
        { "xlt",    "application/vnd.ms-excel"},
        { "xlw",    "application/vnd.ms-excel"},
        { "xof",    "x-world/x-vrml"},
        { "xpm",    "image/x-xpixmap"},
        { "xwd",    "image/x-xwindowdump"},
        { "z",      "application/x-compress"},
        { "zip",    "application/zip"}
    };

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
     * @param part
     * @return
     * @throws IOException
     */
    public static String getTextFromPart(Part part)
    {
        Charset mCharsetConverter;

        try
        {
            if (part != null && part.getBody() != null)
            {
                Body body = part.getBody();
                if (body instanceof TextBody)
                {
                    return ((TextBody)body).getText();
                }
                else
                {
                    InputStream in = part.getBody().getInputStream();
                    String mimeType = part.getMimeType();
                    if (mimeType != null && MimeUtility.mimeTypeMatches(mimeType, "text/*"))
                    {
                        /*
                         * Now we read the part into a buffer for further processing. Because
                         * the stream is now wrapped we'll remove any transfer encoding at this point.
                         */
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        IOUtils.copy(in, out);

                        byte[] bytes = out.toByteArray();
                        in.close();
                        out.close();

                        String charset = getHeaderParameter(part.getContentType(), "charset");
                        /*
                         * We've got a text part, so let's see if it needs to be processed further.
                         */
                        if (charset != null)
                        {
                            /*
                             * See if there is conversion from the MIME charset to the Java one.
                             */
                            mCharsetConverter = Charset.forName(charset);
                            charset = mCharsetConverter.name();
                        }
                        if (charset != null)
                        {
                            /*
                             * We've got a charset encoding, so decode using it.
                             */
                            return new String(bytes, 0, bytes.length, charset);
                        }
                        else
                        {
                            /*
                             * No encoding, so use us-ascii, which is the standard.
                             */
                            return new String(bytes, 0, bytes.length, "ASCII");
                        }
                    }
                }
            }//if text body
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
        boolean attachment = ("attachment".equalsIgnoreCase(dispositionType))
                             || (dispositionFilename != null)
                             && (!"inline".equalsIgnoreCase(dispositionType));

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
        else if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/html")))
        {
            viewables.add(part);
        }
        /*
         * If the part is plain text and it got this far it's part of a
         * mixed (et al) and should be rendered inline.
         */
        else if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/plain")))
        {
            viewables.add(part);
        }
        /*
         * Finally, if it's nothing else we will include it as an attachment.
         */
        else
        {
            attachments.add(part);
        }
    }

    public static String getMimeTypeByExtension(String filename)
    {
        if (filename!=null
                && filename.lastIndexOf('.')!=-1)
        {
            String extension = filename.substring(filename.lastIndexOf('.')+1).toLowerCase();
            for (String[] contentTypeMapEntry : MIME_TYPE_BY_EXTENSION_MAP)
            {
                if (contentTypeMapEntry[0].equals(extension))
                {
                    return contentTypeMapEntry[1];
                }
            }
        }
        return DEFAULT_ATTACHMENT_MIME_TYPE;
    }
}
