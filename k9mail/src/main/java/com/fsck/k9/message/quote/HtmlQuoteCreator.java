package com.fsck.k9.message.quote;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.Resources;
import timber.log.Timber;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.message.html.HtmlConverter;


public class HtmlQuoteCreator {
    // Regular expressions to look for various HTML tags. This is no HTML::Parser, but hopefully it's good enough for
    // our purposes.
    private static final Pattern FIND_INSERTION_POINT_HTML = Pattern.compile("(?si:.*?(<html(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HEAD = Pattern.compile("(?si:.*?(<head(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_BODY = Pattern.compile("(?si:.*?(<body(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HTML_END = Pattern.compile("(?si:.*(</html>).*?)");
    private static final Pattern FIND_INSERTION_POINT_BODY_END = Pattern.compile("(?si:.*(</body>).*?)");
    // The first group in a Matcher contains the first capture group. We capture the tag found in the above REs so that
    // we can locate the *end* of that tag.
    private static final int FIND_INSERTION_POINT_FIRST_GROUP = 1;
    // HTML bits to insert as appropriate
    // TODO is it safe to assume utf-8 here?
    private static final String FIND_INSERTION_POINT_HTML_CONTENT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n<html>";
    private static final String FIND_INSERTION_POINT_HTML_END_CONTENT = "</html>";
    private static final String FIND_INSERTION_POINT_HEAD_CONTENT = "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"></head>";
    // Index of the start of the beginning of a String.
    private static final int FIND_INSERTION_POINT_START_OF_STRING = 0;

    
    /**
     * Add quoting markup to a HTML message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Modified insertable message.
     * @throws MessagingException
     */
    public static InsertableHtmlContent quoteOriginalHtmlMessage(Resources resources, Message originalMessage,
            String messageBody, QuoteStyle quoteStyle) throws MessagingException {
        InsertableHtmlContent insertable = findInsertionPoints(messageBody);

        String sentDate = QuoteHelper.getSentDateText(resources, originalMessage);
        String fromAddress = Address.toString(originalMessage.getFrom());
        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder header = new StringBuilder(QuoteHelper.QUOTE_BUFFER_LENGTH);
            header.append("<div class=\"gmail_quote\">");
            if (sentDate.length() != 0) {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                        resources.getString(R.string.message_compose_reply_header_fmt_with_date), sentDate, fromAddress)
                ));
            } else {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                        resources.getString(R.string.message_compose_reply_header_fmt), fromAddress)
                ));
            }
            header.append("<blockquote class=\"gmail_quote\" " +
                    "style=\"margin: 0pt 0pt 0pt 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">\r\n");

            String footer = "</blockquote></div>";

            insertable.insertIntoQuotedHeader(header.toString());
            insertable.insertIntoQuotedFooter(footer);
        } else if (quoteStyle == QuoteStyle.HEADER) {

            StringBuilder header = new StringBuilder();
            header.append("<div style='font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\";padding:3.0pt 0in 0in 0in'>\r\n");
            header.append("<hr style='border:none;border-top:solid #E1E1E1 1.0pt'>\r\n"); // This gets converted into a horizontal line during html to text conversion.
            if (originalMessage.getFrom() != null && fromAddress.length() != 0) {
                header.append("<b>").append(resources.getString(R.string.message_compose_quote_header_from)).append("</b> ")
                        .append(HtmlConverter.textToHtmlFragment(fromAddress))
                        .append("<br>\r\n");
            }
            if (sentDate.length() != 0) {
                header.append("<b>").append(resources.getString(R.string.message_compose_quote_header_send_date)).append("</b> ")
                        .append(sentDate)
                        .append("<br>\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                header.append("<b>").append(resources.getString(R.string.message_compose_quote_header_to)).append("</b> ")
                        .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.TO))))
                        .append("<br>\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                header.append("<b>").append(resources.getString(R.string.message_compose_quote_header_cc)).append("</b> ")
                        .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.CC))))
                        .append("<br>\r\n");
            }
            if (originalMessage.getSubject() != null) {
                header.append("<b>").append(resources.getString(R.string.message_compose_quote_header_subject)).append("</b> ")
                        .append(HtmlConverter.textToHtmlFragment(originalMessage.getSubject()))
                        .append("<br>\r\n");
            }
            header.append("</div>\r\n");
            header.append("<br>\r\n");

            insertable.insertIntoQuotedHeader(header.toString());
        }

        return insertable;
    }

    /**
     * <p>Find the start and end positions of the HTML in the string. This should be the very top
     * and bottom of the displayable message. It returns a {@link InsertableHtmlContent}, which
     * contains both the insertion points and potentially modified HTML. The modified HTML should be
     * used in place of the HTML in the original message.</p>
     *
     * <p>This method loosely mimics the HTML forward/reply behavior of BlackBerry OS 4.5/BIS 2.5,
     * which in turn mimics Outlook 2003 (as best I can tell).</p>
     *
     * @param content Content to examine for HTML insertion points
     * @return Insertion points and HTML to use for insertion.
     */
    private static InsertableHtmlContent findInsertionPoints(final String content) {
        InsertableHtmlContent insertable = new InsertableHtmlContent();

        // If there is no content, don't bother doing any of the regex dancing.
        if (content == null || content.equals("")) {
            return insertable;
        }

        // Search for opening tags.
        boolean hasHtmlTag = false;
        boolean hasHeadTag = false;
        boolean hasBodyTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlMatcher = FIND_INSERTION_POINT_HTML.matcher(content);
        if (htmlMatcher.matches()) {
            hasHtmlTag = true;
        }
        // Look for a HEAD tag.  If we're missing a BODY tag, we'll use the close of the HEAD to start our content.
        Matcher headMatcher = FIND_INSERTION_POINT_HEAD.matcher(content);
        if (headMatcher.matches()) {
            hasHeadTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to start our content.
        Matcher bodyMatcher = FIND_INSERTION_POINT_BODY.matcher(content);
        if (bodyMatcher.matches()) {
            hasBodyTag = true;
        }

        Timber.d("Open: hasHtmlTag:%s hasHeadTag:%s hasBodyTag:%s", hasHtmlTag, hasHeadTag, hasBodyTag);

        // Given our inspections, let's figure out where to start our content.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just after it.
        if (hasBodyTag) {
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(bodyMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHeadTag) {
            // Now search for a HEAD tag.  We can insert after there.

            // If BlackBerry sees a HEAD tag, it inserts right after that, so long as there is no BODY tag. It doesn't
            // try to add BODY, either.  Right or wrong, it seems to work fine.
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(headMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlTag) {
            // Lastly, check for an HTML tag.
            // In this case, it will add a HEAD, but no BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Insert the HEAD content just after the HTML tag.
            newContent.insert(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP), FIND_INSERTION_POINT_HEAD_CONTENT);
            insertable.setQuotedContent(newContent);
            // The new insertion point is the end of the HTML tag, plus the length of the HEAD content.
            insertable.setHeaderInsertionPoint(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP) + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        } else {
            // If we have none of the above, we probably have a fragment of HTML.  Yahoo! and Gmail both do this.
            // Again, we add a HEAD, but not BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Add the HTML and HEAD tags.
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HEAD_CONTENT);
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HTML_CONTENT);
            // Append the </HTML> tag.
            newContent.append(FIND_INSERTION_POINT_HTML_END_CONTENT);
            insertable.setQuotedContent(newContent);
            insertable.setHeaderInsertionPoint(FIND_INSERTION_POINT_HTML_CONTENT.length() + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        }

        // Search for closing tags. We have to do this after we deal with opening tags since it may
        // have modified the message.
        boolean hasHtmlEndTag = false;
        boolean hasBodyEndTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlEndMatcher = FIND_INSERTION_POINT_HTML_END.matcher(insertable.getQuotedContent());
        if (htmlEndMatcher.matches()) {
            hasHtmlEndTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to place our footer.
        Matcher bodyEndMatcher = FIND_INSERTION_POINT_BODY_END.matcher(insertable.getQuotedContent());
        if (bodyEndMatcher.matches()) {
            hasBodyEndTag = true;
        }

        Timber.d("Close: hasHtmlEndTag:%s hasBodyEndTag:%s", hasHtmlEndTag, hasBodyEndTag);

        // Now figure out where to put our footer.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just before it.
        if (hasBodyEndTag) {
            insertable.setFooterInsertionPoint(bodyEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlEndTag) {
            // Check for an HTML tag.  Add ourselves just before it.
            insertable.setFooterInsertionPoint(htmlEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else {
            // If we have none of the above, we probably have a fragment of HTML.
            // Set our footer insertion point as the end of the string.
            insertable.setFooterInsertionPoint(insertable.getQuotedContent().length());
        }

        return insertable;
    }
}
