package com.fsck.k9.ui.settings.account;


import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.dialog.ApgDeprecationWarningDialog;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpProviderUtil;
import timber.log.Timber;


public class OpenPgpAppSelectDialog extends FragmentActivity {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain";
    private static final String PACKAGE_NAME_APG = "org.thialfihar.android.apg";
    private static final String APG_PROVIDER_PLACEHOLDER = "apg-placeholder";

    public static final String FRAG_OPENPGP_SELECT = "openpgp_select";
    public static final String FRAG_APG_DEPRECATE = "apg_deprecate";
    public static final String FRAG_OPENKEYCHAIN_INFO = "openkeychain_info";

    private static final Intent MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format("market://details?id=%s", OPENKEYCHAIN_PACKAGE)));
    private static final Intent MARKET_INTENT_FALLBACK = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format("https://play.google.com/store/apps/details?id=%s", OPENKEYCHAIN_PACKAGE)));

    private boolean isStopped;
    private Account account;

    public static void startOpenPgpChooserActivity(Context context, Account account) {
        Intent i = new Intent(context, OpenPgpAppSelectDialog.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);
    }

    @Override
    protected void onStart() {
        isStopped = false;
        super.onStart();

        List<String> openPgpProviderPackages = OpenPgpProviderUtil.getOpenPgpProviderPackages(this);
        if (openPgpProviderPackages.isEmpty()) {
            showOpenKeychainInfoFragment();
        } else if (openPgpProviderPackages.size() == 1) {
            Timber.d("Only one OpenPGP provider - just choosing that one!");
            persistOpenPgpProviderSetting(openPgpProviderPackages.get(0));
            finish();
        } else {
            showOpenPgpSelectDialogFragment();
        }
    }

    @Override
    protected void onStop() {
        isStopped = true;
        super.onStop();
    }

    private void showOpenPgpSelectDialogFragment() {
        OpenPgpAppSelectFragment fragment = new OpenPgpAppSelectFragment();
        fragment.show(getSupportFragmentManager(), FRAG_OPENPGP_SELECT);
    }

    private void showApgDeprecationDialogFragment() {
        ApgDeprecationDialogFragment fragment = new ApgDeprecationDialogFragment();
        fragment.show(getSupportFragmentManager(), FRAG_APG_DEPRECATE);
    }

    private void showOpenKeychainInfoFragment() {
        OpenKeychainInfoFragment fragment = new OpenKeychainInfoFragment();
        fragment.show(getSupportFragmentManager(), FRAG_OPENKEYCHAIN_INFO);
    }

    public static class OpenPgpAppSelectFragment extends DialogFragment {
        private ArrayList<OpenPgpProviderEntry> openPgpProviderList = new ArrayList<>();
        private String selectedPackage;

        private void populateAppList() {
            openPgpProviderList.clear();

            Context context = getActivity();

            OpenPgpProviderEntry noneEntry = new OpenPgpProviderEntry(null,
                    context.getString(R.string.openpgp_list_preference_none),
                    getResources().getDrawable(R.drawable.ic_action_cancel_launchersize_light));
            openPgpProviderList.add(noneEntry);

            if (isApgInstalled(getActivity())) {
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

                    if (!OpenPgpProviderUtil.isBlacklisted(packageName)) {
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

        private boolean isApgInstalled(Context context) {
            Intent intent = new Intent("org.openintents.openpgp.IOpenPgpService");
            intent.setPackage(PACKAGE_NAME_APG);
            List<ResolveInfo> resInfo = context.getPackageManager().queryIntentServices(intent, 0);
            return resInfo != null && !resInfo.isEmpty();
        }

        @Override
        public void onStop() {
            super.onStop();

            dismiss();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.account_settings_crypto_app_select_title);

            // do again, maybe an app has now been installed
            populateAppList();

            // Init ArrayAdapter with OpenPGP Providers
            ListAdapter adapter = new ArrayAdapter<OpenPgpProviderEntry>(getActivity(),
                    R.layout.select_openpgp_app_item, android.R.id.text1, openPgpProviderList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    // User super class to create the View
                    View v = super.getView(position, convertView, parent);

                    Drawable icon = openPgpProviderList.get(position).icon;
                    ImageView iconView = v.findViewById(android.R.id.icon1);
                    iconView.setImageDrawable(icon);

                    if (position == 0) {
                        ((CheckedTextView) v.findViewById(android.R.id.text1)).setChecked(true);
                    }

                    return v;
                }
            };

            builder.setSingleChoiceItems(adapter, -1,
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
        public void onStop() {
            super.onStop();

            dismiss();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            ((OpenPgpAppSelectDialog) getActivity()).onDismissApgDialog();
        }
    }

    public static class OpenKeychainInfoFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.dialog_openkeychain_info_title);
            builder.setView(LayoutInflater.from(getActivity()).inflate(
                    R.layout.dialog_openkeychain_info, null, false));

            builder.setNegativeButton(R.string.cancel_action, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            });
            builder.setPositiveButton(R.string.dialog_openkeychain_info_install, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startOpenKeychainInstallActivity();
                    dismiss();
                }
            });

            return builder.create();
        }

        private void startOpenKeychainInstallActivity() {
            try {
                startActivity(MARKET_INTENT);
            } catch (ActivityNotFoundException anfe) {
                startActivity(MARKET_INTENT_FALLBACK);
            }
        }

        @Override
        public void onStop() {
            super.onStop();

            dismiss();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            getActivity().finish();
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
        account.setOpenPgpProvider(selectedPackage);
        account.save();
    }

    public void onDismissApgDialog() {
        if (!isStopped) {
            showOpenPgpSelectDialogFragment();
        }
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
