package com.fsck.k9.preferences.migration;


import java.net.URI;

import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.filter.Base64;
import net.thunderbird.core.logging.legacy.Log;


public class StorageMigrationTo2 {
    public static void urlEncodeUserNameAndPassword(SQLiteDatabase db, StorageMigrationHelper migrationsHelper) {
        Log.i("Updating preferences to urlencoded username/password");

        String accountUuids = migrationsHelper.readValue(db, "accountUuids");
        if (accountUuids != null && accountUuids.length() != 0) {
            String[] uuids = accountUuids.split(",");
            for (String uuid : uuids) {
                try {
                    String storeUriStr = Base64.decode(migrationsHelper.readValue(db, uuid + ".storeUri"));
                    String transportUriStr = Base64.decode(migrationsHelper.readValue(db, uuid + ".transportUri"));

                    URI uri = new URI(transportUriStr);
                    String newUserInfo = null;
                    if (transportUriStr != null) {
                        String[] userInfoParts = uri.getUserInfo().split(":");

                        String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);
                        String passwordEnc = "";
                        String authType = "";
                        if (userInfoParts.length > 1) {
                            passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                        }
                        if (userInfoParts.length > 2) {
                            authType = ":" + userInfoParts[2];
                        }

                        newUserInfo = usernameEnc + passwordEnc + authType;
                    }

                    if (newUserInfo != null) {
                        URI newUri = new URI(uri.getScheme(), newUserInfo, uri.getHost(), uri.getPort(), uri.getPath(),
                                uri.getQuery(), uri.getFragment());
                        String newTransportUriStr = Base64.encode(newUri.toString());
                        migrationsHelper.writeValue(db, uuid + ".transportUri", newTransportUriStr);
                    }

                    uri = new URI(storeUriStr);
                    newUserInfo = null;
                    if (storeUriStr.startsWith("imap")) {
                        String[] userInfoParts = uri.getUserInfo().split(":");
                        if (userInfoParts.length == 2) {
                            String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);
                            String passwordEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[1]);

                            newUserInfo = usernameEnc + ":" + passwordEnc;
                        } else {
                            String authType = userInfoParts[0];
                            String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                            String passwordEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[2]);

                            newUserInfo = authType + ":" + usernameEnc + ":" + passwordEnc;
                        }
                    } else if (storeUriStr.startsWith("pop3")) {
                        String[] userInfoParts = uri.getUserInfo().split(":", 2);
                        String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);

                        String passwordEnc = "";
                        if (userInfoParts.length > 1) {
                            passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                        }

                        newUserInfo = usernameEnc + passwordEnc;
                    } else if (storeUriStr.startsWith("webdav")) {
                        String[] userInfoParts = uri.getUserInfo().split(":", 2);
                        String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);

                        String passwordEnc = "";
                        if (userInfoParts.length > 1) {
                            passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                        }

                        newUserInfo = usernameEnc + passwordEnc;
                    }

                    if (newUserInfo != null) {
                        URI newUri = new URI(uri.getScheme(), newUserInfo, uri.getHost(), uri.getPort(), uri.getPath(),
                                uri.getQuery(), uri.getFragment());
                        String newStoreUriStr = Base64.encode(newUri.toString());
                        migrationsHelper.writeValue(db, uuid + ".storeUri", newStoreUriStr);
                    }
                } catch (Exception e) {
                    Log.e(e, "ooops");
                }
            }
        }
    }
}
