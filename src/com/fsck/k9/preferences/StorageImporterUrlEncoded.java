package com.fsck.k9.preferences;

import java.net.URLDecoder;
import java.util.List;

import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.preferences.StorageImporter.ImportElement;

/**
 * This implementation is provided as a base for development of an unencrypted settings
 * preservation file.  This version validates neither the keys nor values being imported
 * from an easily user editable file, but probably should.
 */

public class StorageImporterUrlEncoded extends BaseStorageImporter implements IStorageImporter {
    public void importPreferences(SharedPreferences.Editor editor, ImportElement dataset, String encryptionKey) throws StorageImportExportException {
        
        List<ImportElement> elements = dataset.subElements;
        Log.i(K9.LOG_TAG, "Got " + elements.size() + " sub-elements");
        for (ImportElement element : elements) {
            String keyEnc = element.attributes.get("key");
            String valueEnc = element.data.toString();
            String key = URLDecoder.decode(keyEnc);
            String value = URLDecoder.decode(valueEnc);
            incorporate(editor, key, value);
        }  
    }

    @Override
    public boolean needsKey() {
        return false;
    }
}
