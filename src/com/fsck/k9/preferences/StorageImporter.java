package com.fsck.k9.preferences;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.DateFormatter;

public class StorageImporter {
    public static int importPreferences(Context context, String fileName, String encryptionKey) throws StorageImportExportException {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
        } catch (FileNotFoundException fnfe) {
            throw new StorageImportExportException("Failure opening settings file " + fileName, fnfe);
        }

        try {
            int count = importPreferences(context, is, encryptionKey);
            return count;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    Log.i(K9.LOG_TAG, "Unable to close InputStream for file " + fileName + ": " + e.getLocalizedMessage());
                }
            }
        }

    }
    public static int importPreferences(Context context, InputStream is, String encryptionKey) throws StorageImportExportException {
        try {
            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();
            SharedPreferences.Editor editor = storage.edit();

            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            StorageImporterHandler handler = new StorageImporterHandler();
            xr.setContentHandler(handler);

            xr.parse(new InputSource(is));

            Element dataset = handler.getRootElement();
            String version = dataset.attributes.get("version");
            Log.i(K9.LOG_TAG, "Got settings file version " + version);

            IStorageImporter storageImporter = null;
            if ("1".equals(version)) {
                storageImporter = new StorageImporterVersion1();
            } else {
                throw new StorageImportExportException("Unable to read file of version " + version
                                                       + "; (only version 1 is readable)");
            }
            int numAccounts = 0;
            if (storageImporter != null) {
                String data = dataset.data.toString();
                numAccounts = storageImporter.importPreferences(preferences, editor, data, encryptionKey);
            }
            editor.commit();
            Preferences.getPreferences(context).refreshAccounts();
            DateFormatter.clearChosenFormat();
            K9.loadPrefs(Preferences.getPreferences(context));
            return numAccounts;
        } catch (SAXException se) {
            throw new StorageImportExportException("Failure reading settings file", se);
        } catch (IOException ie) {
            throw new StorageImportExportException("Failure reading settings file", ie);
        } catch (ParserConfigurationException pce) {
            throw new StorageImportExportException("Failure reading settings file", pce);
        }
    }

    private static class Element {
        String name;
        Map<String, String> attributes = new HashMap<String, String>();
        Map<String, Element> subElements = new HashMap<String, Element>();
        StringBuilder data = new StringBuilder();
    }

    private static class StorageImporterHandler extends DefaultHandler {
        private Element rootElement = new Element();
        private Stack<Element> mOpenTags = new Stack<Element>();

        public Element getRootElement() {
            return this.rootElement;
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
            /* Do nothing */
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes attributes) throws SAXException {
            Log.i(K9.LOG_TAG, "Starting element " + localName);
            Element element = new Element();
            element.name = localName;
            mOpenTags.push(element);
            for (int i = 0; i < attributes.getLength(); i++) {
                String key = attributes.getLocalName(i);
                String value = attributes.getValue(i);
                Log.i(K9.LOG_TAG, "Got attribute " + key + " = " + value);
                element.attributes.put(key, value);
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            Log.i(K9.LOG_TAG, "Ending element " + localName);
            Element element = mOpenTags.pop();
            Element superElement = mOpenTags.empty() ? null : mOpenTags.peek();
            if (superElement != null) {
                superElement.subElements.put(element.name, element);
            } else {
                rootElement = element;
            }
        }

        @Override
        public void characters(char ch[], int start, int length) {
            String value = new String(ch, start, length);
            mOpenTags.peek().data.append(value);
        }
    }
}
