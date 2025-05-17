package com.fsck.k9.ui.settings.account;


import java.util.ArrayList;
import java.util.List;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import net.thunderbird.core.android.account.LegacyAccount;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.base.K9Activity;
import com.fsck.k9.ui.base.ThemeType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpProviderUtil;
import timber.log.Timber;


public class OpenPgpAppSelectDialog extends K9Activity {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain";

    public static final String FRAG_OPENPGP_SELECT = "openpgp_select";
    public static final String FRAG_OPENKEYCHAIN_INFO = "openkeychain_info";

    private static final Intent MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format("market://details?id=%s", OPENKEYCHAIN_PACKAGE)));
    private static final Intent MARKET_INTENT_FALLBACK = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format("https://play.google.com/store/apps/details?id=%s", OPENKEYCHAIN_PACKAGE)));


    private LegacyAccount account;

    public static void startOpenPgpChooserActivity(Context context, LegacyAccount account) {
        Intent i = new Intent(context, OpenPgpAppSelectDialog.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    public OpenPgpAppSelectDialog() {
        super(ThemeType.DIALOG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences().getAccount(accountUuid);
    }

    @Override
    protected void onStart() {
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
        super.onStop();
    }

    private void showOpenPgpSelectDialogFragment() {
        OpenPgpAppSelectFragment fragment = new OpenPgpAppSelectFragment();
        fragment.show(getSupportFragmentManager(), FRAG_OPENPGP_SELECT);
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
                    context.getString(org.openintents.openpgp.R.string.openpgp_list_preference_none),
                    getResources().getDrawable(org.openintents.openpgp.R.drawable.ic_action_cancel_launchersize_light));
            openPgpProviderList.add(noneEntry);

            // search for OpenPGP providers...
            Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
            List<ResolveInfo> resInfo = getActivity().getPackageManager().queryIntentServices(intent, 0);
            boolean hasAllowedChoices = false;
            if (resInfo != null) {
                for (ResolveInfo resolveInfo : resInfo) {
                    if (resolveInfo.serviceInfo == null) {
                        continue;
                    }

                    String packageName = resolveInfo.serviceInfo.packageName;
                    String simpleName = String.valueOf(resolveInfo.serviceInfo.loadLabel(context.getPackageManager()));
                    Drawable icon = resolveInfo.serviceInfo.loadIcon(context.getPackageManager());

                    if (OpenPgpProviderUtil.isProviderAllowed(packageName)) {
                        openPgpProviderList.add(new OpenPgpProviderEntry(packageName, simpleName, icon));
                        hasAllowedChoices = true;
                    }
                }
            }

            if (!hasAllowedChoices) {
                // add install links if provider list is empty
                resInfo = context.getPackageManager().queryIntentActivities(MARKET_INTENT, 0);
                for (ResolveInfo resolveInfo : resInfo) {
                    Intent marketIntent = new Intent(MARKET_INTENT);
                    marketIntent.setPackage(resolveInfo.activityInfo.packageName);
                    Drawable icon = resolveInfo.activityInfo.loadIcon(context.getPackageManager());
                    String marketName = String.valueOf(resolveInfo.activityInfo.applicationInfo
                            .loadLabel(context.getPackageManager()));
                    String simpleName = String.format(
                            context.getString(org.openintents.openpgp.R.string.openpgp_install_openkeychain_via),
                            marketName);
                    openPgpProviderList.add(new OpenPgpProviderEntry(OPENKEYCHAIN_PACKAGE, simpleName,
                            icon, marketIntent));
                }
            }
        }

        @Override
        public void onStop() {
            super.onStop();

            dismissAllowingStateLoss();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());

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

    public static class OpenKeychainInfoFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

            builder.setTitle(R.string.dialog_openkeychain_info_title);
            builder.setView(getLayoutInflater().inflate(
                    R.layout.dialog_openkeychain_info, null, false));

            builder.setNegativeButton(com.fsck.k9.ui.base.R.string.cancel_action, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            });
            builder.setPositiveButton(R.string.dialog_openkeychain_info_install, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                    startOpenKeychainInstallActivity();
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

            dismissAllowingStateLoss();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            getActivity().finish();
        }
    }

    public void onSelectProvider(String selectedPackage) {
        persistOpenPgpProviderSetting(selectedPackage);
        finish();
    }

    private void persistOpenPgpProviderSetting(String selectedPackage) {
        account.setOpenPgpProvider(selectedPackage);
        Preferences.getPreferences().saveAccount(account);
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
