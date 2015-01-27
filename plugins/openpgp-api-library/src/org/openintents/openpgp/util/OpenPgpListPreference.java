/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
 */

package org.openintents.openpgp.util;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.openintents.openpgp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Does not extend ListPreference, but is very similar to it!
 * http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.4_r1/android/preference/ListPreference.java/?v=source
 */
public class OpenPgpListPreference extends DialogPreference {
    private static final String OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain";
    private static final String MARKET_INTENT_URI_BASE = "market://details?id=%s";
    private static final Intent MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format(MARKET_INTENT_URI_BASE, OPENKEYCHAIN_PACKAGE)));

    private ArrayList<OpenPgpProviderEntry> mLegacyList = new ArrayList<OpenPgpProviderEntry>();
    private ArrayList<OpenPgpProviderEntry> mList = new ArrayList<OpenPgpProviderEntry>();

    private String mSelectedPackage;

    public OpenPgpListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenPgpListPreference(Context context) {
        this(context, null);
    }

    /**
     * Public method to add new entries for legacy applications
     *
     * @param packageName
     * @param simpleName
     * @param icon
     */
    public void addLegacyProvider(int position, String packageName, String simpleName, Drawable icon) {
        mLegacyList.add(position, new OpenPgpProviderEntry(packageName, simpleName, icon));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        mList.clear();
        
        // add "none"-entry
        mList.add(0, new OpenPgpProviderEntry("",
                getContext().getString(R.string.openpgp_list_preference_none),
                getContext().getResources().getDrawable(R.drawable.ic_action_cancel_launchersize)));
        
        // add all additional (legacy) providers
        mList.addAll(mLegacyList);
        
        // search for OpenPGP providers...
        ArrayList<OpenPgpProviderEntry> providerList = new ArrayList<OpenPgpProviderEntry>();
        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT);
        List<ResolveInfo> resInfo = getContext().getPackageManager().queryIntentServices(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo.loadLabel(getContext()
                        .getPackageManager()));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(getContext().getPackageManager());

                providerList.add(new OpenPgpProviderEntry(packageName, simpleName, icon));
            }
        }

        if (providerList.isEmpty()) {
            // add install links if provider list is empty
            resInfo = getContext().getPackageManager().queryIntentActivities
                    (MARKET_INTENT, 0);
            for (ResolveInfo resolveInfo : resInfo) {
                Intent marketIntent = new Intent(MARKET_INTENT);
                marketIntent.setPackage(resolveInfo.activityInfo.packageName);
                Drawable icon = resolveInfo.activityInfo.loadIcon(getContext().getPackageManager());
                String marketName = String.valueOf(resolveInfo.activityInfo.applicationInfo
                        .loadLabel(getContext().getPackageManager()));
                String simpleName = String.format(getContext().getString(R.string
                        .openpgp_install_openkeychain_via), marketName);
                mList.add(new OpenPgpProviderEntry(OPENKEYCHAIN_PACKAGE, simpleName,
                        icon, marketIntent));
            }
        } else {
            // add provider
            mList.addAll(providerList);
        }

        // Init ArrayAdapter with OpenPGP Providers
        ListAdapter adapter = new ArrayAdapter<OpenPgpProviderEntry>(getContext(),
                android.R.layout.select_dialog_singlechoice, android.R.id.text1, mList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                // User super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);

                // Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(mList.get(position).icon, null,
                        null, null);

                // Add margin between image and text (support various screen densities)
                int dp10 = (int) (10 * getContext().getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp10);

                return v;
            }
        };

        builder.setSingleChoiceItems(adapter, getIndexOfProviderList(getValue()),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OpenPgpProviderEntry entry = mList.get(which);

                        if (entry.intent != null) {
                            /*
                             * Intents are called as activity
                             *
                             * Current approach is to assume the user installed the app.
                             * If he does not, the selected package is not valid.
                             *
                             * However  applications should always consider this could happen,
                             * as the user might remove the currently used OpenPGP app.
                             */
                            getContext().startActivity(entry.intent);
                        }

                        mSelectedPackage = entry.packageName;

                        /*
                         * Clicking on an item simulates the positive button click, and dismisses
                         * the dialog.
                         */
                        OpenPgpListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        /*
         * The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
         * dialog instead of the user having to press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && (mSelectedPackage != null)) {
            if (callChangeListener(mSelectedPackage)) {
                setValue(mSelectedPackage);
            }
        }
    }

    private int getIndexOfProviderList(String packageName) {
        for (OpenPgpProviderEntry app : mList) {
            if (app.packageName.equals(packageName)) {
                return mList.indexOf(app);
            }
        }

        return -1;
    }

    public void setValue(String packageName) {
        mSelectedPackage = packageName;
        persistString(packageName);
    }

    public String getValue() {
        return mSelectedPackage;
    }

    public String getEntry() {
        return getEntryByValue(mSelectedPackage);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString(mSelectedPackage) : (String) defaultValue);
    }

    public String getEntryByValue(String packageName) {
        for (OpenPgpProviderEntry app : mList) {
            if (app.packageName.equals(packageName)) {
                return app.simpleName;
            }
        }

        return null;
    }

    private static class OpenPgpProviderEntry {
        private String packageName;
        private String simpleName;
        private Drawable icon;
        private Intent intent;

        public OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.icon = icon;
        }

        public OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon, Intent intent) {
            this(packageName, simpleName, icon);
            this.intent = intent;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }
}
