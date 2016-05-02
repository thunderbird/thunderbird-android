package com.fsck.k9.mailstore;

import com.fsck.k9.mail.Part;

import java.util.List;

import android.content.Context;


/**
 * Store viewable text of a message as plain text and HTML, and the parts considered
 * attachments.
 *
 * @see MessageViewInfoExtractor#extractTextAndAttachments(Context, Part, List) (Context, List, List)
 */
public class ViewableContainer {
    /**
     * The viewable text of the message in plain text.
     */
    public final String text;

    /**
     * The viewable text of the message in HTML.
     */
    public final String html;

    public ViewableContainer(String text, String html) {
        this.text = text;
        this.html = html;
    }
}
