package com.fsck.k9.activity.setup.checksettings;


import java.security.cert.X509Certificate;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsContract.Presenter;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsPresenter.CheckDirection;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class CheckSettingsView implements CheckSettingsContract.View,
        ConfirmationDialogFragmentListener {

    public static final int ACTIVITY_REQUEST_CODE = 1;

    private static final String EXTRA_EMAIL = "email";
    private static final String EXTRA_PASSWORD = "password";
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_CHECK_DIRECTION ="checkDirection";
    private static final String EXTRA_AUTOCONFIGURATION = "autocongifuration";

    private Presenter presenter;
    private AbstractAccountSetup activity;
    private boolean destroyed;
    private boolean canceled;
    private TextView messageView;
    private MaterialProgressBar progressBar;

    private String email;
    private String password;

    private Handler handler;

    public CheckSettingsView(AbstractAccountSetup activity) {
        setActivity(activity);

        messageView = (TextView) activity.findViewById(R.id.message);
        progressBar = (MaterialProgressBar) activity.findViewById(R.id.progress);

        progressBar.setIndeterminate(true);

        handler = new Handler(Looper.getMainLooper());

        Account account = activity.getState().getAccount();
        String email = activity.getState().getEmail();
        String password = activity.getState().getPassword();



        // String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        // account = Preferences.getPreferences(this).getAccount(accountUuid);


    }

    @Override
    public void start() {
        presenter.onViewStart(activity.getState());



        /* if (activity.getState().getStep() == AccountState.STEP_AUTO_CONFIGURATION) {
        }
        if (getIntent().getBooleanExtra(EXTRA_AUTOCONFIGURATION, false)) {
            email = getIntent().getStringExtra(EXTRA_EMAIL);
            password = getIntent().getStringExtra(EXTRA_PASSWORD);
            presenter = new CheckSettingsPresenter(this, account);
            presenter.autoConfiguration(email, password);
        } else {
            CheckDirection direction = (CheckDirection) getIntent().getSerializableExtra(EXTRA_CHECK_DIRECTION);
            presenter = new CheckSettingsPresenter(this, account, direction);
            presenter.checkSettings();
        } */
    }

    // FIXME: 7/24/2017 change it
    public static void startChecking(Activity activity, String accountUuid, CheckDirection checkDirection) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, accountUuid);
        bundle.putSerializable(EXTRA_CHECK_DIRECTION, checkDirection);

        Intent intent = new Intent(activity, CheckSettingsView.class);
        intent.putExtras(bundle);

        activity.startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void autoConfigurationFail() {
        activity.goToManualSetup();
    }

    @Override
    public void showAcceptKeyDialog(final int msgResId, final String exMessage, final String message,
            final X509Certificate certificate) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: refactor with DialogFragment.
                // This is difficult because we need to pass through chain[0] for onClick()
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                        .setMessage(activity.getString(msgResId, exMessage)
                                + " " + message
                        )
                        .setCancelable(true)
                        .setPositiveButton(
                                activity.getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        presenter.onCertificateAccepted(certificate);
                                    }
                                })
                        .setNegativeButton(
                                activity.getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        activity.finish();
                                    }
                                })
                        .show();
            }
        });
    }

    @Override
    public void showErrorDialog(@StringRes final int msgResId, final Object... args) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                showDialogFragment(R.id.dialog_account_setup_error, activity.getString(msgResId, args));
            }
        });
    }

    @Override
    public boolean canceled() {
        return canceled | destroyed;
    }

    @Override
    public void setMessage(@StringRes int id) {
        messageView.setText(activity.getString(id));
    }

    @Override
    public void goToBasics() {
        activity.goToBasics();
    }

    @Override
    public void goToIncoming() {
        activity.goToIncoming();
    }

    @Override
    public void goToOutgoing() {
        activity.goToOutgoing();
    }

    @Override
    public void goToNames() {
        activity.goToAccountNames();
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        if (destroyed) {
            return;
        }

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        activity.getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        activity.getString(R.string.account_setup_failed_dlg_edit_details_action),
                        activity.getString(R.string.account_setup_failed_dlg_continue_action)
                );
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        FragmentTransaction ta = activity.getFragmentManager().beginTransaction();
        ta.add(fragment, getDialogTag(dialogId));
        ta.commitAllowingStateLoss();

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        //fragment.show(ta, getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                presenter.onError();
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                presenter.skip();
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
    }
}
