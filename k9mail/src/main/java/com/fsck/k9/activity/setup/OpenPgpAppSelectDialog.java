package com.fsck.k9.activity.setup;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.ui.dialog.ApgDeprecationWarningDialog;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpAppPreference;


public class OpenPgpAppSelectDialog extends Activity {
    private static final String OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain";
    private static final String APG_PROVIDER_PLACEHOLDER = "apg-placeholder";
    private static final String PACKAGE_NAME_APG = "org.thialfihar.android.apg";

    public static final String FRAG_OPENPGP_SELECT = "openpgp_select";
    public static final String FRAG_APG_DEPRECATE = "apg_deprecate";

    private static final String MARKET_INTENT_URI_BASE = "market://details?id=%s";
    private static final Intent MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format(MARKET_INTENT_URI_BASE, OPENKEYCHAIN_PACKAGE)));

    private static final ArrayList<String> PROVIDER_BLACKLIST = new ArrayList<>();

    static {
        // Unfortunately, the current released version of APG includes a broken version of the API
        PROVIDER_BLACKLIST.add(PACKAGE_NAME_APG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);

        if (savedInstanceState == null) {
            showOpenPgpSelectDialogFragment();
        }
    }

    private void showOpenPgpSelectDialogFragment() {
        OpenPgpAppSelectFragment fragment = new OpenPgpAppSelectFragment();
        fragment.show(getFragmentManager(), FRAG_OPENPGP_SELECT);
    }

    private void showApgDeprecationDialogFragment() {
        ApgDeprecationDialogFragment fragment = new ApgDeprecationDialogFragment();
        fragment.show(getFragmentManager(), FRAG_APG_DEPRECATE);
    }

    public static class OpenPgpAppSelectFragment extends DialogFragment {
        private ArrayList<OpenPgpProviderEntry> openPgpProviderList = new ArrayList<>();
        private String selectedPackage;

        private void populateAppList() {
            openPgpProviderList.clear();

            Context context = getActivity();

            OpenPgpProviderEntry noneEntry = new OpenPgpProviderEntry("",
                    context.getString(R.string.openpgp_list_preference_none),
                    getResources().getDrawable(R.drawable.ic_action_cancel_launchersize));
            openPgpProviderList.add(0, noneEntry);

            if (OpenPgpAppPreference.isApgInstalled(getActivity())) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_apg_small);
                openPgpProviderList.add(new OpenPgpProviderEntry(
                        APG_PROVIDER_PLACEHOLDER, getString(R.string.apg), icon));
            }

            // search for OpenPGP providers...
            Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
            List<ResolveInfo> resInfo = getActivity().getPackageManager().queryIntentServices(intent, 0);
            boolean hasNonBlacklistedChoices = false;
            if (resInfo != null) {
                for (ResolveInfo resolveInfo : resInfo) {
                    if (resolveInfo.serviceInfo == null) {
                        continue;
                    }

                    String packageName = resolveInfo.serviceInfo.packageName;
                    String simpleName = String.valueOf(resolveInfo.serviceInfo.loadLabel(context.getPackageManager()));
                    Drawable icon = resolveInfo.serviceInfo.loadIcon(context.getPackageManager());

                    if (!PROVIDER_BLACKLIST.contains(packageName)) {
                        openPgpProviderList.add(new OpenPgpProviderEntry(packageName, simpleName, icon));
                        hasNonBlacklistedChoices = true;
                    }
                }
            }

            if (!hasNonBlacklistedChoices) {
                // add install links if provider list is empty
                resInfo = context.getPackageManager().queryIntentActivities(MARKET_INTENT, 0);
                for (ResolveInfo resolveInfo : resInfo) {
                    Intent marketIntent = new Intent(MARKET_INTENT);
                    marketIntent.setPackage(resolveInfo.activityInfo.packageName);
                    Drawable icon = resolveInfo.activityInfo.loadIcon(context.getPackageManager());
                    String marketName = String.valueOf(resolveInfo.activityInfo.applicationInfo
                            .loadLabel(context.getPackageManager()));
                    String simpleName = String.format(context.getString(R.string
                            .openpgp_install_openkeychain_via), marketName);
                    openPgpProviderList.add(new OpenPgpProviderEntry(OPENKEYCHAIN_PACKAGE, simpleName,
                            icon, marketIntent));
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            selectedPackage = K9.getOpenPgpProvider();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.account_settings_crypto_app);

            // do again, maybe an app has now been installed
            populateAppList();

            // Init ArrayAdapter with OpenPGP Providers
            ListAdapter adapter = new ArrayAdapter<OpenPgpProviderEntry>(getActivity(),
                    android.R.layout.select_dialog_singlechoice, android.R.id.text1, openPgpProviderList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    // User super class to create the View
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);

                    // Put the image on the TextView
                    tv.setCompoundDrawablesWithIntrinsicBounds(openPgpProviderList.get(position).icon, null,
                            null, null);

                    // Add margin between image and text (support various screen densities)
                    int dp10 = (int) (10 * getContext().getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp10);

                    return v;
                }
            };

            builder.setSingleChoiceItems(adapter, getIndexOfProviderList(selectedPackage),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OpenPgpProviderEntry entry = openPgpProviderList.get(which);

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
                                getActivity().startActivity(entry.intent);
                                return;
                            }

                            selectedPackage = entry.packageName;

                            dialog.dismiss();
                        }
                    });

            return builder.create();
        }

        private int getIndexOfProviderList(String packageName) {
            for (OpenPgpProviderEntry app : openPgpProviderList) {
                if (app.packageName.equals(packageName)) {
                    return openPgpProviderList.indexOf(app);
                }
            }

            // default is "none"
            return 0;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            ((OpenPgpAppSelectDialog) getActivity()).onSelectProvider(selectedPackage);
        }
    }

    public static class ApgDeprecationDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new ApgDeprecationWarningDialog(getActivity());
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            ((OpenPgpAppSelectDialog) getActivity()).onDismissApgDialog();
        }
    }

    public void onSelectProvider(String selectedPackage) {
        if (APG_PROVIDER_PLACEHOLDER.equals(selectedPackage)) {
            showApgDeprecationDialogFragment();
            return;
        }

        persistOpenPgpProviderSetting(selectedPackage);
        finish();
    }

    private void persistOpenPgpProviderSetting(String selectedPackage) {
        K9.setOpenPgpProvider(selectedPackage);

        StorageEditor editor = Preferences.getPreferences(this).getStorage().edit();
        K9.save(editor);
        editor.commit();
    }

    public void onDismissApgDialog() {
        showOpenPgpSelectDialogFragment();
    }

    private static class OpenPgpProviderEntry {
        private String packageName;
        private String simpleName;
        private Drawable icon;
        private Intent intent;

        OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.icon = icon;
        }

        OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon, Intent intent) {
            this(packageName, simpleName, icon);
            this.intent = intent;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }
}
