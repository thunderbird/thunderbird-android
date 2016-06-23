package com.fsck.k9.service;


import java.io.File;
import java.io.IOException;

import android.net.Uri;


public interface FileProviderInterface {

    File createProvidedFile() throws IOException;
    Uri getUriForProvidedFile(File file, String mimeType) throws IOException;

}
