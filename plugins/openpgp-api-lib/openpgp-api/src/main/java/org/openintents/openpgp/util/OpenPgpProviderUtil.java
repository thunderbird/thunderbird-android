package org.openintents.openpgp.util;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;


public class OpenPgpProviderUtil {
    public static List<String> getOpenPgpProviderPackages(Context context) {
        ArrayList<String> result = new ArrayList<>();

        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentServices(intent, 0);
        if (resInfo == null) {
            return result;
        }

        for (ResolveInfo resolveInfo : resInfo) {
            if (resolveInfo.serviceInfo == null) {
                continue;
            }

            result.add(resolveInfo.serviceInfo.packageName);
        }

        return result;
    }

    public static String getOpenPgpProviderName(PackageManager packageManager, String openPgpProvider) {
        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
        intent.setPackage(openPgpProvider);
        List<ResolveInfo> resInfo = packageManager.queryIntentServices(intent, 0);
        if (resInfo == null) {
            return null;
        }

        for (ResolveInfo resolveInfo : resInfo) {
            if (resolveInfo.serviceInfo == null) {
                continue;
            }

            return String.valueOf(resolveInfo.serviceInfo.loadLabel(packageManager));
        }

        return null;
    }
}
