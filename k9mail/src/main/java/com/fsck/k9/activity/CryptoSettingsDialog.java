package com.fsck.k9.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.fsck.k9.R;
import com.fsck.k9.activity.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.CryptoStatusSelector;
import com.fsck.k9.view.CryptoStatusSelector.CryptoStatusSelectedListener;
import com.fsck.k9.view.LinearViewAnimator;


public class CryptoSettingsDialog extends DialogFragment implements CryptoStatusSelectedListener {

    public static final String ARG_CURRENT_MODE = "current_override";

    private CryptoStatusSelector cryptoStatusSelector;
    private LinearViewAnimator cryptoStatusText;

    private CryptoMode currentMode;

    public static CryptoSettingsDialog newInstance(CryptoMode initialOverride) {
        CryptoSettingsDialog dialog = new CryptoSettingsDialog();

        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_MODE, initialOverride.toString());
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.crypto_settings_dialog, null);
        cryptoStatusSelector = (CryptoStatusSelector) view.findViewById(R.id.crypto_status_selector);
        cryptoStatusText = (LinearViewAnimator) view.findViewById(R.id.crypto_status_text);

        cryptoStatusSelector.setCryptoStatusListener(this);

        Bundle arguments = savedInstanceState != null ? savedInstanceState : getArguments();
        currentMode = CryptoMode.valueOf(arguments.getString(ARG_CURRENT_MODE));
        updateView(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton("Proceed", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    void updateView(boolean animate) {
        switch (currentMode) {
            case DISABLE:
                cryptoStatusSelector.setCryptoStatus(0);
                cryptoStatusText.setDisplayedChild(0, animate);
                break;
            case SIGN_ONLY:
                cryptoStatusSelector.setCryptoStatus(1);
                cryptoStatusText.setDisplayedChild(1, animate);
                break;
            case OPPORTUNISTIC:
                cryptoStatusSelector.setCryptoStatus(2);
                cryptoStatusText.setDisplayedChild(2, animate);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_CURRENT_MODE, currentMode.toString());
    }

    @Override
    public void onCryptoStatusSelected(int status) {
        if (status == 0) {
            currentMode = CryptoMode.DISABLE;
        } else if (status == 1) {
            currentMode = CryptoMode.SIGN_ONLY;
        } else {
            currentMode = CryptoMode.OPPORTUNISTIC;
        }
        updateView(true);

        Activity activity = getActivity();
        if (activity == null) {
            // is this supposed to happen?
            return;
        }

        ((MessageCompose) activity).onCryptoModeChanged(currentMode);
    }

}
