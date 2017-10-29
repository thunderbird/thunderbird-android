package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;

import java.util.List;

/**
 * Empty marker class interface the class hierarchy used by
 * {@link MessageExtractor#findViewablesAndAttachments(com.fsck.k9.mail.Part, java.util.List, java.util.List)}
 *
 * @see Viewable.Text
 * @see Viewable.Html
 * @see Viewable.MessageHeader
 * @see Viewable.Alternative
 */
public interface Viewable {
    /**
     * Class representing textual parts of a message that aren't marked as attachments.
     *
     * @see com.fsck.k9.mail.internet.MessageExtractor#isPartTextualBody(com.fsck.k9.mail.Part)
     */
    abstract class Textual implements Viewable {
        private Part part;

        Textual(Part part) {
            this.part = part;
        }

        public Part getPart() {
            return part;
        }
    }

    /**
     * Class representing a {@code text/plain} part of a message.
     */
    class Text extends Textual {
        public Text(Part part) {
            super(part);
        }
    }

    class Flowed extends Textual {
        private boolean delSp;

        Flowed(Part part, boolean delSp) {
            super(part);
            this.delSp = delSp;
        }

        public boolean isDelSp() {
            return delSp;
        }
    }

    /**
     * Class representing a {@code text/html} part of a message.
     */
    class Html extends Textual {
        Html(Part part) {
            super(part);
        }
    }

    /**
     * Class representing a {@code message/rfc822} part of a message.
     *
     * <p>
     * This is used to extract basic header information when the message contents are displayed
     * inline.
     * </p>
     */
    class MessageHeader implements Viewable {
        private Part containerPart;
        private Message message;

        public MessageHeader(Part containerPart, Message message) {
            this.containerPart = containerPart;
            this.message = message;
        }

        public Part getContainerPart() {
            return containerPart;
        }

        public Message getMessage() {
            return message;
        }
    }

    /**
     * Class representing a {@code multipart/alternative} part of a message.
     *
     * <p>
     * Only relevant {@code text/plain} and {@code text/html} children are stored in this container
     * class.
     * </p>
     */
    class Alternative implements Viewable {
        private List<Viewable> text;
        private List<Viewable> html;

        Alternative(List<Viewable> text, List<Viewable> html) {
            this.text = text;
            this.html = html;
        }

        public List<Viewable> getText() {
            return text;
        }

        public List<Viewable> getHtml() {
            return html;
        }
    }
}
