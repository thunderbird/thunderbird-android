package com.fsck.k9.ui.messageview;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.ThemeUtils;


public class CryptoInfoDialog extends DialogFragment {
    public static final String ARG_DISPLAY_STATUS = "display_status";
    public static final String ARG_HAS_SECURITY_WARNING = "has_security_warning";
    public static final int ICON_ANIM_DELAY = 400;
    public static final int ICON_ANIM_DURATION = 350;


    private View dialogView;

    private View topIconFrame;
    private ImageView topIcon_1;
    private ImageView topIcon_2;
    private ImageView topIcon_3;
    private TextView topText;

    private View bottomIconFrame;
    private ImageView bottomIcon_1;
    private ImageView bottomIcon_2;
    private TextView bottomText;


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

        dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.message_crypto_info_dialog, null);

        topIconFrame = dialogView.findViewById(R.id.crypto_info_top_frame);
        topIcon_1 = (ImageView) topIconFrame.findViewById(R.id.crypto_info_top_icon_1);
        topIcon_2 = (ImageView) topIconFrame.findViewById(R.id.crypto_info_top_icon_2);
        topIcon_3 = (ImageView) topIconFrame.findViewById(R.id.crypto_info_top_icon_3);
        topText = (TextView) dialogView.findViewById(R.id.crypto_info_top_text);

        bottomIconFrame = dialogView.findViewById(R.id.crypto_info_bottom_frame);
        bottomIcon_1 = (ImageView) bottomIconFrame.findViewById(R.id.crypto_info_bottom_icon_1);
        bottomIcon_2 = (ImageView) bottomIconFrame.findViewById(R.id.crypto_info_bottom_icon_2);
        bottomText = (TextView) dialogView.findViewById(R.id.crypto_info_bottom_text);

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
                    if (! (frag instanceof OnClickShowCryptoKeyListener)) {
                        throw new AssertionError("Displaying activity must implement OnClickShowCryptoKeyListener!");
                    }
                    ((OnClickShowCryptoKeyListener) frag).onClickShowSecurityWarning();
                }
            });
        } else if (displayStatus.hasAssociatedKey()) {
            b.setNeutralButton(R.string.crypto_info_view_key, new OnClickListener() {
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
        if (displayStatus.textResTop == null) {
            throw new AssertionError("Crypto info dialog can only be displayed for items with text!");
        }

        if (displayStatus.textResBottom == null) {
            setMessageSingleLine(displayStatus.colorAttr,
                    displayStatus.textResTop, displayStatus.statusIconRes,
                    displayStatus.statusDotsRes);
        } else {
            if (displayStatus.statusDotsRes == null) {
                throw new AssertionError("second icon must be non-null if second text is non-null!");
            }
            setMessageWithAnimation(displayStatus.colorAttr,
                    displayStatus.textResTop, displayStatus.statusIconRes,
                    displayStatus.textResBottom, displayStatus.statusDotsRes);
        }
    }

    private void setMessageSingleLine(@AttrRes int colorAttr,
            @StringRes int topTextRes, @DrawableRes int statusIconRes,
            @DrawableRes Integer statusDotsRes) {
        @ColorInt int color = ThemeUtils.getStyledColor(getActivity(), colorAttr);

        topIcon_1.setImageResource(statusIconRes);
        topIcon_1.setColorFilter(color);
        topText.setText(topTextRes);

        if (statusDotsRes != null) {
            topIcon_3.setImageResource(statusDotsRes);
            topIcon_3.setColorFilter(color);
            topIcon_3.setVisibility(View.VISIBLE);
        } else {
            topIcon_3.setVisibility(View.GONE);
        }

        bottomText.setVisibility(View.GONE);
        bottomIconFrame.setVisibility(View.GONE);
    }

    private void setMessageWithAnimation(@AttrRes int colorAttr,
            @StringRes int topTextRes, @DrawableRes int statusIconRes,
            @StringRes int bottomTextRes, @DrawableRes int statusDotsRes) {
        topIcon_1.setImageResource(statusIconRes);
        topIcon_2.setImageResource(statusDotsRes);
        topIcon_3.setVisibility(View.GONE);
        topText.setText(topTextRes);

        bottomIcon_1.setImageResource(statusIconRes);
        bottomIcon_2.setImageResource(statusDotsRes);
        bottomText.setText(bottomTextRes);

        topIcon_1.setColorFilter(ThemeUtils.getStyledColor(getActivity(), colorAttr));
        bottomIcon_2.setColorFilter(ThemeUtils.getStyledColor(getActivity(), colorAttr));

        prepareIconAnimation();
    }

    private void prepareIconAnimation() {
        topText.setAlpha(0.0f);
        bottomText.setAlpha(0.0f);

        dialogView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                float halfVerticalPixelDifference = (bottomIconFrame.getY() - topIconFrame.getY()) / 2.0f;
                topIconFrame.setTranslationY(halfVerticalPixelDifference);
                bottomIconFrame.setTranslationY(-halfVerticalPixelDifference);

                topIconFrame.animate().translationY(0)
                        .setStartDelay(ICON_ANIM_DELAY)
                        .setDuration(ICON_ANIM_DURATION)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                bottomIconFrame.animate().translationY(0)
                        .setStartDelay(ICON_ANIM_DELAY)
                        .setDuration(ICON_ANIM_DURATION)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                topText.animate().alpha(1.0f).setStartDelay(ICON_ANIM_DELAY + ICON_ANIM_DURATION).start();
                bottomText.animate().alpha(1.0f).setStartDelay(ICON_ANIM_DELAY + ICON_ANIM_DURATION).start();

                view.removeOnLayoutChangeListener(this);
            }
        });
    }

    public interface OnClickShowCryptoKeyListener {
        void onClickShowCryptoKey();
        void onClickShowSecurityWarning();
    }
}
