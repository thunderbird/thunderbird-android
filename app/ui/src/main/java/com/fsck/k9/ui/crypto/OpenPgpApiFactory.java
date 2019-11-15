package com.fsck.k9.ui.crypto;


import android.content.Context;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.remote.OpenPgpService;


public class OpenPgpApiFactory {
    OpenPgpApi createOpenPgpApi(Context context, OpenPgpService service) {
        return new OpenPgpApi(context, service);
    }
}
