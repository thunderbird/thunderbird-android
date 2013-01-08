/*
 * Copyright (C) 2012 Christian Ketterer (cketti)
 *
 * Portions Copyright (C) 2012 Martin van Zuilekom (http://martin.cubeactive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Based on android-change-log:
 *
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * http://code.google.com/p/android-change-log/
 */
package de.cketti.library.changelog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.WebView;


/**
 * Display a dialog showing a full or partial (What's New) change log.
 */
public class ChangeLog {
    /**
     * Tag that is used when sending error/debug messages to the log.
     */
    protected static final String LOG_TAG = "ckChangeLog";

    /**
     * This is the key used when storing the version code in SharedPreferences.
     */
    protected static final String VERSION_KEY = "ckChangeLog_last_version_code";

    /**
     * Constant that used when no version code is available.
     */
    protected static final int NO_VERSION = -1;

    /**
     * Default CSS styles used to format the change log.
     */
    private static final String DEFAULT_CSS =
            "h1 { margin-left: 0px; font-size: 1.2em;}" +
            "li { margin-left: 0px;}" +
            "ul { padding-left: 2em;}";


    /**
     * Context that is used to access the resources and to create the ChangeLog dialogs.
     */
    protected final Context mContext;

    /**
     * Contains the CSS rules used to format the change log.
     */
    protected final String mCss;

    /**
     * Last version code read from {@code SharedPreferences} or {@link #NO_VERSION}.
     */
    private int mLastVersionCode;

    /**
     * Version code of the current installation.
     */
    private int mCurrentVersionCode;

    /**
     * Version name of the current installation.
     */
    private String mCurrentVersionName;


    /**
     * Contains constants for the root element of {@code changelog.xml}.
     */
    protected interface ChangeLogTag {
        static final String NAME = "changelog";
    }

    /**
     * Contains constants for the release element of {@code changelog.xml}.
     */
    protected interface ReleaseTag {
        static final String NAME = "release";
        static final String ATTRIBUTE_VERSION = "version";
        static final String ATTRIBUTE_VERSION_CODE = "versioncode";
    }

    /**
     * Contains constants for the change element of {@code changelog.xml}.
     */
    protected interface ChangeTag {
        static final String NAME = "change";
    }

    /**
     * Create a {@code ChangeLog} instance using the default {@link SharedPreferences} file.
     *
     * @param context
     *         Context that is used to access the resources and to create the ChangeLog dialogs.
     */
    public ChangeLog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context), DEFAULT_CSS);
    }

    /**
     * Create a {@code ChangeLog} instance using the default {@link SharedPreferences} file.
     *
     * @param context
     *         Context that is used to access the resources and to create the ChangeLog dialogs.
     * @param css
     *         CSS styles that will be used to format the change log.
     */
    public ChangeLog(Context context, String css) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context), css);
    }

    /**
     * Create a {@code ChangeLog} instance using the supplied {@code SharedPreferences} instance.
     *
     * @param context
     *         Context that is used to access the resources and to create the ChangeLog dialogs.
     * @param preferences
     *         {@code SharedPreferences} instance that is used to persist the last version code.
     * @param css
     *         CSS styles used to format the change log (excluding {@code <style>} and
     *         {@code </style>}).
     *
     */
    public ChangeLog(Context context, SharedPreferences preferences, String css) {
        mContext = context;
        mCss = css;

        // Get last version code
        mLastVersionCode = preferences.getInt(VERSION_KEY, NO_VERSION);

        // Get current version code and version name
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);

            mCurrentVersionCode = packageInfo.versionCode;
            mCurrentVersionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            mCurrentVersionCode = NO_VERSION;
            Log.e(LOG_TAG, "Could not get version information from manifest!", e);
        }
    }

    /**
     * Get version code of last installation.
     *
     * @return The version code of the last installation of this app (as described in the former
     *         manifest). This will be the same as returned by {@link #getCurrentVersionCode()} the
     *         second time this version of the app is launched (more precisely: the second time
     *         {@code ChangeLog} is instantiated).
     *
     * @see AndroidManifest.xml#android:versionCode
     */
    public int getLastVersionCode() {
        return mLastVersionCode;
    }

    /**
     * Get version code of current installation.
     *
     * @return The version code of this app as described in the manifest.
     *
     * @see AndroidManifest.xml#android:versionCode
     */
    public int getCurrentVersionCode() {
        return mCurrentVersionCode;
    }

    /**
     * Get version name of current installation.
     *
     * @return The version name of this app as described in the manifest.
     *
     * @see AndroidManifest.xml#android:versionName
     */
    public String getCurrentVersionName() {
        return mCurrentVersionName;
    }

    /**
     * Check if this is the first execution of this app version.
     *
     * @return {@code true} if this version of your app is started the first time.
     */
    public boolean isFirstRun() {
        return mLastVersionCode < mCurrentVersionCode;
    }

    /**
     * Check if this is a new installation.
     *
     * @return {@code true} if your app including {@code ChangeLog} is started the first time ever.
     *         Also {@code true} if your app was uninstalled and installed again.
     */
    public boolean isFirstRunEver() {
        return mLastVersionCode == NO_VERSION;
    }

    /**
     * Get the "What's New" dialog.
     *
     * @return An AlertDialog displaying the changes since the previous installed version of your
     *         app (What's New). But when this is the first run of your app including
     *         {@code ChangeLog} then the full log dialog is show.
     */
    public AlertDialog getLogDialog() {
        return getDialog(isFirstRunEver());
    }

    /**
     * Get a dialog with the full change log.
     *
     * @return An AlertDialog with a full change log displayed.
     */
    public AlertDialog getFullLogDialog() {
        return getDialog(true);
    }

    /**
     * Create a dialog containing (parts of the) change log.
     *
     * @param full
     *         If this is {@code true} the full change log is displayed. Otherwise only changes for
     *         versions newer than the last version are displayed.
     *
     * @return A dialog containing the (partial) change log.
     */
    protected AlertDialog getDialog(boolean full) {
        WebView wv = new WebView(mContext);
        //wv.setBackgroundColor(0); // transparent
        wv.loadDataWithBaseURL(null, getLog(full), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(
                mContext.getResources().getString(
                        full ? R.string.changelog_full_title : R.string.changelog_title))
                .setView(wv)
                .setCancelable(false)
                // OK button
                .setPositiveButton(
                        mContext.getResources().getString(R.string.changelog_ok_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // The user clicked "OK" so save the current version code as
                                // "last version code".
                                updateVersionInPreferences();
                            }
                        });

        if (!full) {
            // Show "More…" button if we're only displaying a partial change log.
            builder.setNegativeButton(R.string.changelog_show_full,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            getFullLogDialog().show();
                        }
                    });
        }

        return builder.create();
    }

    /**
     * Write current version code to the preferences.
     */
    protected void updateVersionInPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(VERSION_KEY, mCurrentVersionCode);

        // TODO: Update preferences from a background thread
        editor.commit();
    }

    /**
     * Get changes since last version as HTML string.
     *
     * @return HTML string containing the changes since the previous installed version of your app
     *         (What's New).
     */
    public String getLog() {
        return getLog(false);
    }

    /**
     * Get full change log as HTML string.
     *
     * @return HTML string containing the full change log.
     */
    public String getFullLog() {
        return getLog(true);
    }

    /**
     * Get (partial) change log as HTML string.
     *
     * @param full
     *         If this is {@code true} the full change log is returned. Otherwise only changes for
     *         versions newer than the last version are returned.
     *
     * @return The (partial) change log.
     */
    private String getLog(boolean full) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><head><style type=\"text/css\">");
        sb.append(mCss);
        sb.append("</style></head><body>");

        Resources resources = mContext.getResources();

        // Read master change log from raw/changelog.xml
        SparseArray<ReleaseItem> defaultChangelog;
        try {
            XmlPullParser xml = XmlPullParserFactory.newInstance().newPullParser();
            InputStreamReader reader = new InputStreamReader(resources.openRawResource(R.raw.changelog));
            xml.setInput(reader);
            try {
                defaultChangelog = readChangeLog(xml, full);
            } finally {
                try { reader.close(); } catch (Exception e) { /* do nothing */ }
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "Error reading raw/changelog.xml", e);
            return null;
        }

        // Read localized change log from xml[-lang]/changelog.xml
        XmlResourceParser resXml = mContext.getResources().getXml(R.xml.changelog);
        SparseArray<ReleaseItem> changelog;
        try {
            changelog = readChangeLog(resXml, full);
        } finally {
            resXml.close();
        }

        String versionFormat = resources.getString(R.string.changelog_version_format);

        // Get all version codes from the master change log...
        List<Integer> versions = new ArrayList<Integer>(defaultChangelog.size());
        for (int i = 0, len = defaultChangelog.size(); i < len; i++) {
            int key = defaultChangelog.keyAt(i);
            versions.add(key);
        }

        // ... and sort them (newest version first).
        Collections.sort(versions, Collections.reverseOrder());

        for (Integer version : versions) {
            int key = version.intValue();

            // Use release information from localized change log and fall back to the master file
            // if necessary.
            ReleaseItem release = changelog.get(key, defaultChangelog.get(key));

            sb.append("<h1>");
            sb.append(String.format(versionFormat, release.versionName));
            sb.append("</h1><ul>");
            for (String change : release.changes) {
                sb.append("<li>");
                sb.append(change);
                sb.append("</li>");
            }
            sb.append("</ul>");
        }

        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Read the change log from an XML file.
     *
     * @param xml
     *         The {@code XmlPullParser} instance used to read the change log.
     * @param full
     *         If {@code true} the full change log is read. Otherwise only the changes since the
     *         last (saved) version are read.
     *
     * @return A {@code SparseArray} mapping the version codes to release information.
     */
    protected SparseArray<ReleaseItem> readChangeLog(XmlPullParser xml, boolean full) {
        SparseArray<ReleaseItem> result = new SparseArray<ReleaseItem>();

        try {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().equals(ReleaseTag.NAME)) {
                    if (parseReleaseTag(xml, full, result)) {
                        // Stop reading more elements if this entry is not newer than the last
                        // version.
                        break;
                    }
                }
                eventType = xml.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse the {@code release} tag of a change log XML file.
     *
     * @param xml
     *         The {@code XmlPullParser} instance used to read the change log.
     * @param full
     *         If {@code true} the contents of the {@code release} tag are always added to
     *         {@code changelog}. Otherwise only if the item's {@code versioncode} attribute is
     *         higher than the last version code.
     * @param changelog
     *         The {@code SparseArray} to add a new {@link ReleaseItem} instance to.
     *
     * @return {@code true} if the {@code release} element is describing changes of a version older
     *         or equal to the last version. In that case {@code changelog} won't be modified and
     *         {@link #readChangeLog(XmlPullParser, boolean)} will stop reading more elements from
     *         the change log file.
     *
     * @throws XmlPullParserException
     * @throws IOException
     */
    private boolean parseReleaseTag(XmlPullParser xml, boolean full,
            SparseArray<ReleaseItem> changelog) throws XmlPullParserException, IOException {

        String version = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION);

        int versionCode;
        try {
            String versionCodeStr = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION_CODE);
            versionCode = Integer.parseInt(versionCodeStr);
        } catch (NumberFormatException e) {
            versionCode = NO_VERSION;
        }

        if (!full && versionCode <= mLastVersionCode) {
            return true;
        }

        int eventType = xml.getEventType();
        List<String> changes = new ArrayList<String>();
        while (eventType != XmlPullParser.END_TAG || xml.getName().equals(ChangeTag.NAME)) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals(ChangeTag.NAME)) {
                eventType = xml.next();

                changes.add(xml.getText());
            }
            eventType = xml.next();
        }

        ReleaseItem release = new ReleaseItem(versionCode, version, changes);
        changelog.put(versionCode, release);

        return false;
    }

    /**
     * Container used to store information about a release/version.
     */
    protected static class ReleaseItem {
        /**
         * Version code of the release.
         */
        public final int versionCode;

        /**
         * Version name of the release.
         */
        public final String versionName;

        /**
         * List of changes introduced with that release.
         */
        public final List<String> changes;

        ReleaseItem(int versionCode, String versionName, List<String> changes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.changes = changes;
        }
    }
}
