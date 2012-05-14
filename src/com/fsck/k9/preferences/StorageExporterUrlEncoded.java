package com.fsck.k9.preferences;

import java.io.PrintWriter;
import java.net.URLEncoder;

/**
 * This implementation is provided as a base for development of an unencrypted settings
 * preservation file.  This version does not hide or suppress passwords or other sensitive
 * information and therefore may not be suitable for use by users.
 */

public class StorageExporterUrlEncoded extends BaseStorageExporter implements IStorageExporter {
    StringBuilder builder = null;
   
    @Override
    public boolean needsKey() {
        return false;
    }

    @Override
    public void output(PrintWriter pf, String key, String value) throws StorageImportExportException {
        String keyEnc = URLEncoder.encode(key);
        String valueEnc = URLEncoder.encode(value);
        builder.append("<setting key=\"");
        builder.append(keyEnc);
        builder.append("\">");
        builder.append(valueEnc);
        builder.append("</setting>");
        builder.append("\n");
    }

    @Override
    public void initialize(String encryptionKey) throws StorageImportExportException {
        builder = new StringBuilder();
    }

    @Override
    public void finish(PrintWriter pf) throws StorageImportExportException {
        pf.print(builder.toString()); 
    }
}
