package com.fsck.k9.mail.store.webdav;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;

/**
 * Handler for WebDAV XML events
 */
class WebDavHandler extends DefaultHandler {
    private DataSet mDataSet = new DataSet();
    private final LinkedList<String> mOpenTags = new LinkedList<String>();

    public DataSet getDataSet() {
        return this.mDataSet;
    }

    @Override
    public void startDocument() throws SAXException {
        this.mDataSet = new DataSet();
    }

    @Override
    public void endDocument() throws SAXException {
            /* Do nothing */
    }

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {
        mOpenTags.addFirst(localName);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        mOpenTags.removeFirst();

        /** Reset the hash temp variables */
        if (localName.equals("response")) {
            this.mDataSet.finish();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String value = new String(ch, start, length);
        mDataSet.addValue(value, mOpenTags.peek());
    }
}
