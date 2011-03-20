package com.fsck.k9.preferences;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.AsyncUIProcessor;
import com.fsck.k9.activity.ImportListener;
import com.fsck.k9.activity.PasswordEntryDialog;
import com.fsck.k9.helper.DateFormatter;

public class StorageImporter {

    public static void importPreferences(Activity activity, InputStream is, String providedEncryptionKey, ImportListener listener) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            StorageImporterHandler handler = new StorageImporterHandler();
            xr.setContentHandler(handler);

            xr.parse(new InputSource(is));

            ImportElement dataset = handler.getRootElement();
            String version = dataset.attributes.get("version");
            Log.i(K9.LOG_TAG, "Got settings file version " + version);

            IStorageImporter storageImporter = StorageVersioning.createImporter(version);
            if (storageImporter == null) {
                throw new StorageImportExportException(activity.getString(R.string.settings_unknown_version, version));
            }
            if (storageImporter.needsKey() && providedEncryptionKey == null) {
                gatherPassword(activity, storageImporter, dataset, listener);
            } else {
                finishImport(activity, storageImporter, dataset, providedEncryptionKey, listener);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.failure(e.getLocalizedMessage(), e);
            }
        }
    }

    private static void finishImport(Activity context, IStorageImporter storageImporter, ImportElement dataset, String encryptionKey, ImportListener listener) throws StorageImportExportException {
        if (listener != null) {
            listener.started();
        }
        Preferences preferences = Preferences.getPreferences(context);
        SharedPreferences storage = preferences.getPreferences();
        SharedPreferences.Editor editor = storage.edit();
        int numAccounts = 0;
        if (storageImporter != null) {
            numAccounts = storageImporter.importPreferences(preferences, editor, dataset, encryptionKey);
        }
        editor.commit();
        Preferences.getPreferences(context).refreshAccounts();
        DateFormatter.clearChosenFormat();
        K9.loadPrefs(Preferences.getPreferences(context));
        K9.setServicesEnabled(context);
        if (listener != null) {
            listener.success(numAccounts);
        }
    }

    private static void gatherPassword(final Activity activity, final IStorageImporter storageImporter, final ImportElement dataset, final ImportListener listener) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PasswordEntryDialog dialog = new PasswordEntryDialog(activity, activity.getString(R.string.settings_encryption_password_prompt),
                new PasswordEntryDialog.PasswordEntryListener() {
                    public void passwordChosen(final String chosenPassword) {
                        AsyncUIProcessor.getInstance(activity.getApplication()).execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    finishImport(activity, storageImporter, dataset, chosenPassword, listener);
                                } catch (Exception e) {
                                    Log.w(K9.LOG_TAG, "Failure during import", e);
                                    if (listener != null) {
                                        listener.failure(e.getLocalizedMessage(), e);
                                    }
                                }
                            }
                        });
                    }

                    public void cancel() {
                        if (listener != null) {
                            listener.canceled();
                        }
                    }
                });

                dialog.show();
            }
        });

    };


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
