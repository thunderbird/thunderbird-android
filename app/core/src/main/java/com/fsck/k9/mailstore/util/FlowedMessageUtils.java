package com.fsck.k9.mailstore.util;


/**
 * Adapted from the Apache James project, see
 * https://james.apache.org/mailet/base/apidocs/org/apache/mailet/base/FlowedMessageUtils.html
 *
 * <p>Manages texts encoded as <code>text/plain; format=flowed</code>.</p>
 * <p>As a reference see:</p>
 * <ul>
 * <li><a href='http://www.rfc-editor.org/rfc/rfc2646.txt'>RFC2646</a></li>
 * <li><a href='http://www.rfc-editor.org/rfc/rfc3676.txt'>RFC3676</a> (new method with DelSP support).
 * </ul>
 * <h4>Note</h4>
 * <ul>
 * <li>In order to decode, the input text must belong to a mail with headers similar to:
 *   Content-Type: text/plain; charset="CHARSET"; [delsp="yes|no"; ]format="flowed"
 *   (the quotes around CHARSET are not mandatory).
 *   Furthermore the header Content-Transfer-Encoding MUST NOT BE Quoted-Printable
 *   (see RFC3676 paragraph 4.2).(In fact this happens often for non 7bit messages).
 * </li>
 * </ul>
 */
public final class FlowedMessageUtils {
    private static final char RFC2646_SPACE = ' ';
    private static final char RFC2646_QUOTE = '>';
    private static final String RFC2646_SIGNATURE = "-- ";
    private static final String RFC2646_CRLF = "\r\n";

    private FlowedMessageUtils() {
        // this class cannot be instantiated
    }

    /**
     * Decodes a text previously wrapped using "format=flowed".
     */
    public static String deflow(String text, boolean delSp) {
        String[] lines = text.split("\r\n|\n", -1);
        StringBuffer result = null;
        StringBuffer resultLine = new StringBuffer();
        int resultLineQuoteDepth = 0;
        boolean resultLineFlowed = false;
        // One more cycle, to close the last line
        for (int i = 0; i <= lines.length; i++) {
            String line = i < lines.length ? lines[i] : null;
            int actualQuoteDepth = 0;

            if (line != null && line.length() > 0) {
                if (line.equals(RFC2646_SIGNATURE))
                    // signature handling (the previous line is not flowed)
                    resultLineFlowed = false;

                else if (line.charAt(0) == RFC2646_QUOTE) {
                    // Quote
                    actualQuoteDepth = 1;
                    while (actualQuoteDepth < line.length() && line.charAt(actualQuoteDepth) == RFC2646_QUOTE) actualQuoteDepth ++;
                    // if quote-depth changes wrt the previous line then this is not flowed
                    if (resultLineQuoteDepth != actualQuoteDepth) resultLineFlowed = false;
                    line = line.substring(actualQuoteDepth);

                } else {
                    // id quote-depth changes wrt the first line then this is not flowed
                    if (resultLineQuoteDepth > 0) resultLineFlowed = false;
                }

                if (line.length() > 0 && line.charAt(0) == RFC2646_SPACE)
                    // Line space-stuffed
                    line = line.substring(1);

                // if the previous was the last then it was not flowed
            } else if (line == null) resultLineFlowed = false;

            // Add the PREVIOUS line.
            // This often will find the flow looking for a space as the last char of the line.
            // With quote changes or signatures it could be the followinf line to void the flow.
            if (!resultLineFlowed && i > 0) {
                if (resultLineQuoteDepth > 0) resultLine.insert(0, RFC2646_SPACE);
                for (int j = 0; j < resultLineQuoteDepth; j++) resultLine.insert(0, RFC2646_QUOTE);
                if (result == null) result = new StringBuffer();
                else result.append(RFC2646_CRLF);
                result.append(resultLine.toString());
                resultLine = new StringBuffer();
                resultLineFlowed = false;
            }
            resultLineQuoteDepth = actualQuoteDepth;

            if (line != null) {
                if (!line.equals(RFC2646_SIGNATURE) && line.endsWith("" + RFC2646_SPACE) && i < lines.length - 1) {
                    // Line flowed (NOTE: for the split operation the line having i == lines.length is the last that does not end with RFC2646_CRLF)
                    if (delSp) line = line.substring(0, line.length() - 1);
                    resultLineFlowed = true;
                }

                else resultLineFlowed = false;

                resultLine.append(line);
            }
        }

        return result.toString();
    }
}
