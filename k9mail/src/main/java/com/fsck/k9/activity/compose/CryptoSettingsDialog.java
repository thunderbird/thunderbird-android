package com.fsck.k9.activity.compose;


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
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.CryptoModeSelector;
import com.fsck.k9.view.CryptoModeSelector.CryptoModeSelectorState;
import com.fsck.k9.view.CryptoModeSelector.CryptoStatusSelectedListener;
import com.fsck.k9.view.LinearViewAnimator;


public class CryptoSettingsDialog extends DialogFragment implements CryptoStatusSelectedListener {
    private static final String ARG_CURRENT_MODE = "current_mode";


    private CryptoModeSelector cryptoModeSelector;
    private LinearViewAnimator cryptoStatusText;

    private CryptoMode currentMode;


    public static CryptoSettingsDialog newInstance(CryptoMode initialMode) {
        CryptoSettingsDialog dialog = new CryptoSettingsDialog();

        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_MODE, initialMode.toString());
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = savedInstanceState != null ? savedInstanceState : getArguments();
        currentMode = CryptoMode.valueOf(arguments.getString(ARG_CURRENT_MODE));

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.crypto_settings_dialog, null);
        cryptoModeSelector = (CryptoModeSelector) view.findViewById(R.id.crypto_status_selector);
        cryptoStatusText = (LinearViewAnimator) view.findViewById(R.id.crypto_status_text);

        cryptoModeSelector.setCryptoStatusListener(this);

        updateView(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.crypto_settings_cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.crypto_settings_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeCryptoSettings();
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    private void changeCryptoSettings() {
        Activity activity = getActivity();
        if (activity == null) {
            // is this supposed to happen?
            return;
        }
        boolean activityIsCryptoModeChangedListener = activity instanceof OnCryptoModeChangedListener;
        if (!activityIsCryptoModeChangedListener) {
            throw new AssertionError("This dialog must be called by an OnCryptoModeChangedListener!");
        }

        ((OnCryptoModeChangedListener) activity).onCryptoModeChanged(currentMode);
    }

    void updateView(boolean animate) {
        switch (currentMode) {
            case DISABLE:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.DISABLED);
                cryptoStatusText.setDisplayedChild(0, animate);
                break;
            case SIGN_ONLY:
                throw new IllegalStateException("This state can't be set here!");
            case OPPORTUNISTIC:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.OPPORTUNISTIC);
                cryptoStatusText.setDisplayedChild(1, animate);
                break;
            case PRIVATE:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.PRIVATE);
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
    public void onCryptoStatusSelected(CryptoModeSelectorState status) {
        switch (status) {
            case DISABLED:
                currentMode = CryptoMode.DISABLE;
                break;
            case SIGN_ONLY:
                throw new IllegalStateException("This widget doesn't support sign-only state!");
            case OPPORTUNISTIC:
                currentMode = CryptoMode.OPPORTUNISTIC;
                break;
            case PRIVATE:
                currentMode = CryptoMode.PRIVATE;
                break;
        }
        updateView(true);
    }

    public interface OnCryptoModeChangedListener {
        void onCryptoModeChanged(CryptoMode cryptoMode);
    }

}
