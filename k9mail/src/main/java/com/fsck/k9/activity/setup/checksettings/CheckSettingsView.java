package com.fsck.k9.activity.setup.checksettings;


import java.security.cert.X509Certificate;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.AccountSetupView;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsContract.Presenter;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class CheckSettingsView extends AccountSetupView implements CheckSettingsContract.View, ConfirmationDialogFragmentListener {


    private static final int ACTIVITY_REQUEST_CODE = 1;

    private static final String EXTRA_EMAIL = "email";
    private static final String EXTRA_PASSWORD = "password";
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_CHECK_DIRECTION ="checkDirection";
    private static final String EXTRA_AUTOCONFIGURATION = "autocongifuration";

    private Presenter presenter;
    private TextView messageView;
    private MaterialProgressBar progressBar;

    private Handler handler;

    public CheckSettingsView(AbstractAccountSetup activity) {
        super(activity);
    }

    @Override
    public void start() {
        messageView = (TextView) activity.findViewById(R.id.message);
        progressBar = (MaterialProgressBar) activity.findViewById(R.id.progress);

        progressBar.setIndeterminate(true);

        handler = new Handler(Looper.getMainLooper());

        presenter = new CheckSettingsPresenter(this);
        presenter.onViewStart(activity.getState());
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
    public void onAutoConfigurationFail() {
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
        // TODO: 7/27/17 update later
        return false;
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
    public void goToOptions() {
        activity.goToOptions();
    }

    @Override
    public Context getContext() {
        return activity;
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        // TODO: 7/27/2017 implement it
        /* if (destroyed) {
            return;
        } */

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        activity.getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        activity.getString(R.string.account_setup_failed_dlg_edit_details_action),
                        activity.getString(R.string.account_setup_failed_dlg_continue_action),
                        this
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
        presenter.onPositiveClickedInConfirmationDialog();
    }

    @Override
    public void doNegativeClick(int dialogId) {
        presenter.onNegativeClickedInConfirmationDialog();
    }

    @Override
    public void dialogCancelled(int dialogId) {

    }
}
