package com.fsck.k9.ui.messageview;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.ui.R;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.ThemeUtils;


public class CryptoInfoDialog extends DialogFragment {
    public static final String ARG_DISPLAY_STATUS = "display_status";
    public static final String ARG_HAS_SECURITY_WARNING = "has_security_warning";


    private ImageView statusIcon;
    private TextView titleText;
    private TextView descriptionText;


    public static CryptoInfoDialog newInstance(MessageCryptoDisplayStatus displayStatus, boolean hasSecurityWarning) {
        CryptoInfoDialog frag = new CryptoInfoDialog();

        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_STATUS, displayStatus.toString());
        args.putBoolean(ARG_HAS_SECURITY_WARNING, hasSecurityWarning);
        frag.setArguments(args);

        return frag;
    }

    @SuppressLint("InflateParams") // inflating without root element is fine for creating a dialog
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder b = new AlertDialog.Builder(getActivity());

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.message_crypto_info_dialog, null);

        statusIcon = dialogView.findViewById(R.id.crypto_info_top_icon_1);
        titleText = dialogView.findViewById(R.id.crypto_info_title);
        descriptionText = dialogView.findViewById(R.id.crypto_info_text);

        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.valueOf(getArguments().getString(ARG_DISPLAY_STATUS));
        setMessageForDisplayStatus(displayStatus);

        b.setView(dialogView);
        b.setPositiveButton(R.string.crypto_info_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        boolean hasSecurityWarning = getArguments().getBoolean(ARG_HAS_SECURITY_WARNING);
        if (hasSecurityWarning) {
            b.setNeutralButton(R.string.crypto_info_view_security_warning, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Fragment frag = getTargetFragment();
                    if (!(frag instanceof OnClickShowCryptoKeyListener)) {
                        throw new AssertionError("Displaying activity must implement OnClickShowCryptoKeyListener!");
                    }
                    ((OnClickShowCryptoKeyListener) frag).onClickShowSecurityWarning();
                }
            });
        } else if (displayStatus.isUnknownKey()) {
            b.setNeutralButton(R.string.crypto_info_search_key, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Fragment frag = getTargetFragment();
                    if (! (frag instanceof OnClickShowCryptoKeyListener)) {
                        throw new AssertionError("Displaying activity must implement OnClickShowCryptoKeyListener!");
                    }
                    ((OnClickShowCryptoKeyListener) frag).onClickSearchKey();
                }
            });
        } else if (displayStatus.hasAssociatedKey()) {
            int buttonLabel = displayStatus.isUnencryptedSigned() ?
                    R.string.crypto_info_view_signer : R.string.crypto_info_view_sender;
            b.setNeutralButton(buttonLabel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Fragment frag = getTargetFragment();
                    if (! (frag instanceof OnClickShowCryptoKeyListener)) {
                        throw new AssertionError("Displaying activity must implement OnClickShowCryptoKeyListener!");
                    }
                    ((OnClickShowCryptoKeyListener) frag).onClickShowCryptoKey();
                }
            });
        }

        return b.create();
    }

    private void setMessageForDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        if (displayStatus.titleTextRes == null) {
            throw new AssertionError("Crypto info dialog can only be displayed for items with text!");
        }

        setMessageSingleLine(displayStatus.colorAttr, displayStatus.titleTextRes, displayStatus.descriptionTextRes,
                displayStatus.statusIconRes);
    }

    private void setMessageSingleLine(@AttrRes int colorAttr, @StringRes int titleTextRes,
            @StringRes Integer descTextRes, @DrawableRes int statusIconRes) {
        @ColorInt int color = ThemeUtils.getStyledColor(getActivity(), colorAttr);

        statusIcon.setImageResource(statusIconRes);
        statusIcon.setColorFilter(color);
        titleText.setText(titleTextRes);
        if (descTextRes != null) {
            descriptionText.setText(descTextRes);
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }
    }

    public interface OnClickShowCryptoKeyListener {
        void onClickShowCryptoKey();
        void onClickShowSecurityWarning();
        void onClickSearchKey();
    }
}
