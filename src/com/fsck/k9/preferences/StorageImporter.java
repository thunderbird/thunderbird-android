package com.fsck.k9.preferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

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

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.DateFormatter;

public class StorageImporter {

    public static void importPreferences(Context context, InputStream is, String encryptionKey,
            boolean globalSettings, String[] importAccountUuids, boolean overwrite)
    throws StorageImportExportException {

        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            StorageImporterHandler handler = new StorageImporterHandler();
            xr.setContentHandler(handler);

            xr.parse(new InputSource(is));

            ImportElement dataset = handler.getRootElement();
            String storageFormat = dataset.attributes.get("version");
            Log.i(K9.LOG_TAG, "Got settings file version " + storageFormat);

            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();
            SharedPreferences.Editor editor = storage.edit();

            String data = dataset.data.toString();
            List<Integer> accountNumbers = Account.getExistingAccountNumbers(preferences);
            Log.i(K9.LOG_TAG, "Existing accountNumbers = " + accountNumbers);
            /**
             *  We translate UUIDs in the import file into new UUIDs in the local instance for the following reasons:
             *  1) Accidentally importing the same file twice cannot damage settings in an existing account.
             *     (Say, an account that was imported two months ago and has since had significant settings changes.)
             *  2) Importing a single file multiple times allows for creating multiple accounts from the same template.
             *  3) Exporting an account and importing back into the same instance is a poor-man's account copy (until a real
             *     copy function is created, if ever)
             */
            Map<String, String> uuidMapping = new HashMap<String, String>();
            String accountUuids = preferences.getPreferences().getString("accountUuids", null);

            StringReader sr = new StringReader(data);
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            int settingsImported = 0;
            int numAccounts = 0;
            K9Krypto krypto = new K9Krypto(encryptionKey, K9Krypto.MODE.DECRYPT);
            do {
                line = br.readLine();
                if (line != null) {
                    //Log.i(K9.LOG_TAG, "Got line " + line);
                    String[] comps = line.split(":");
                    if (comps.length > 1) {
                        String keyEnc = comps[0];
                        String valueEnc = comps[1];
                        String key = krypto.decrypt(keyEnc);
                        String value = krypto.decrypt(valueEnc);
                        String[] keyParts = key.split("\\.");
                        if (keyParts.length > 1) {
                            String oldUuid = keyParts[0];
                            String newUuid = uuidMapping.get(oldUuid);
                            if (newUuid == null) {
                                newUuid = UUID.randomUUID().toString();
                                uuidMapping.put(oldUuid, newUuid);

                                Log.i(K9.LOG_TAG, "Mapping oldUuid " + oldUuid + " to newUuid " + newUuid);
                            }
                            keyParts[0] = newUuid;
                            if ("accountNumber".equals(keyParts[1])) {
                                int accountNumber = Account.findNewAccountNumber(accountNumbers);
                                accountNumbers.add(accountNumber);
                                value = Integer.toString(accountNumber);
                                accountUuids += (accountUuids.length() != 0 ? "," : "") + newUuid;
                                numAccounts++;
                            }
                            StringBuilder builder = new StringBuilder();
                            for (String part : keyParts) {
                                if (builder.length() > 0) {
                                    builder.append(".");
                                }
                                builder.append(part);
                            }
                            key = builder.toString();
                        }
                        //Log.i(K9.LOG_TAG, "Setting " + key + " = " + value);
                        settingsImported++;
                        editor.putString(key, value);
                    }
                }

            } while (line != null);

            editor.putString("accountUuids", accountUuids);
            Log.i(K9.LOG_TAG, "Imported " + settingsImported + " settings and " + numAccounts + " accounts");

            editor.commit();
            Preferences.getPreferences(context).refreshAccounts();
            DateFormatter.clearChosenFormat();
            K9.loadPrefs(Preferences.getPreferences(context));
            K9.setServicesEnabled(context);

        } catch (Exception e) {
            throw new StorageImportExportException();
        }
    }


    public static class ImportElement {
        String name;
        Map<String, String> attributes = new HashMap<String, String>();
        Map<String, ImportElement> subElements = new HashMap<String, ImportElement>();
        StringBuilder data = new StringBuilder();
    }

    private static class StorageImporterHandler extends DefaultHandler {
        private ImportElement rootElement = new ImportElement();
        private Stack<ImportElement> mOpenTags = new Stack<ImportElement>();

        public ImportElement getRootElement() {
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
            ImportElement element = new ImportElement();
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
            ImportElement element = mOpenTags.pop();
            ImportElement superElement = mOpenTags.empty() ? null : mOpenTags.peek();
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
