package com.fsck.k9.mail.store.webdav;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;

/**
 * Handler for WebDAV XML events
 */
class WebDavHandler extends DefaultHandler {
    private DataSet dataSet = new DataSet();
    private final LinkedList<String> openTags = new LinkedList<String>();

    public DataSet getDataSet() {
        return this.dataSet;
    }

    @Override
    public void startDocument() throws SAXException {
        this.dataSet = new DataSet();
    }

    @Override
    public void endDocument() throws SAXException {
            /* Do nothing */
    }

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {
        openTags.addFirst(localName);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        openTags.removeFirst();

        /* Reset the hash temp variables */
        if (localName.equals("response")) {
            this.dataSet.finish();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String value = new String(ch, start, length);
        dataSet.addValue(value, openTags.peek());
    }
}
