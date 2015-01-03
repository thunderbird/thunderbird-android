package com.fsck.k9.mailstore;

import com.fsck.k9.mail.Part;

import java.util.List;

/**
 * Store viewable text of a message as plain text and HTML, and the parts considered
 * attachments.
 *
 * @see LocalMessageExtractor#extractTextAndAttachments(android.content.Context, com.fsck.k9.mail.Message)
 */
class ViewableContainer {
    /**
     * The viewable text of the message in plain text.
     */
    public final String text;

    /**
     * The viewable text of the message in HTML.
     */
    public final String html;

    /**
     * The parts of the message considered attachments (everything not viewable).
     */
    public final List<Part> attachments;

    public ViewableContainer(String text, String html, List<Part> attachments) {
        this.text = text;
        this.html = html;
        this.attachments = attachments;
    }
}
