/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class OpenPgpListPreference extends DialogPreference {
    ArrayList<OpenPgpProviderEntry> mProviderList = new ArrayList<OpenPgpProviderEntry>();
    private String mSelectedPackage;

    public static final int REQUIRED_API_VERSION = 1;

    public OpenPgpListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        List<ResolveInfo> resInfo =
                context.getPackageManager().queryIntentServices(
                        new Intent(IOpenPgpService.class.getName()), PackageManager.GET_META_DATA);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo
                        .loadLabel(context.getPackageManager()));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(context.getPackageManager());

                // get api version
                ServiceInfo si = resolveInfo.serviceInfo;
                int apiVersion = si.metaData.getInt("api_version");

                mProviderList.add(new OpenPgpProviderEntry(packageName, simpleName, icon,
                        apiVersion));
            }
        }
    }

    public OpenPgpListPreference(Context context) {
        this(context, null);
    }

    /**
     * Can be used to add "no selection"
     * 
     * @param packageName
     * @param simpleName
     * @param icon
     */
    public void addProvider(int position, String packageName, String simpleName, Drawable icon,
            int apiVersion) {
        mProviderList.add(position, new OpenPgpProviderEntry(packageName, simpleName, icon,
                apiVersion));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        // Init ArrayAdapter with OpenPGP Providers
        ListAdapter adapter = new ArrayAdapter<OpenPgpProviderEntry>(getContext(),
                android.R.layout.select_dialog_singlechoice, android.R.id.text1, mProviderList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                // User super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);

                // Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(mProviderList.get(position).icon,
                        null, null, null);

                // Add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                // disable if it has the wrong api_version
                if (mProviderList.get(position).apiVersion == REQUIRED_API_VERSION) {
                    tv.setEnabled(true);
                } else {
                    tv.setEnabled(false);
                    tv.setText(tv.getText() + " (API v"
                            + mProviderList.get(position).apiVersion + ", needs v"
                            + REQUIRED_API_VERSION + ")");
                }

                return v;
            }
        };

        builder.setSingleChoiceItems(adapter, getIndexOfProviderList(getValue()),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedPackage = mProviderList.get(which).packageName;

                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        OpenPgpListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
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
        for (OpenPgpProviderEntry app : mProviderList) {
            if (app.packageName.equals(packageName)) {
                return mProviderList.indexOf(app);
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

    public String getEntryByValue(String packageName) {
        for (OpenPgpProviderEntry app : mProviderList) {
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
        private int apiVersion;

        public OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon,
                int apiVersion) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.icon = icon;
            this.apiVersion = apiVersion;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }
}
