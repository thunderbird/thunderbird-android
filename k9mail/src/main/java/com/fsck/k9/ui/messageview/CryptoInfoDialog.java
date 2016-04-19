package com.fsck.k9.ui.messageview;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
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


public class CryptoInfoDialog extends DialogFragment {
    public static final String ARG_DISPLAY_STATUS = "display_status";


    private View dialogView;
    private View icon1frame;
    private View icon2frame;
    private TextView text1;
    private TextView text2;
    private ImageView icon_1_1;
    private ImageView icon_1_2;
    private ImageView icon_1_3;
    private ImageView icon_2_1;
    private ImageView icon_2_2;


    public static CryptoInfoDialog newInstance(MessageCryptoDisplayStatus displayStatus) {
        CryptoInfoDialog frag = new CryptoInfoDialog();

        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_STATUS, displayStatus.toString());
        frag.setArguments(args);

        return frag;
    }

    @SuppressLint("InflateParams") // inflating without root element is fine for creating a dialog
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder b = new AlertDialog.Builder(getActivity());

        dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.message_crypto_info_dialog, null);

        icon1frame = dialogView.findViewById(R.id.crypto_info_frame_1);
        icon_1_1 = (ImageView) icon1frame.findViewById(R.id.crypto_info_icon_1_1);
        icon_1_2 = (ImageView) icon1frame.findViewById(R.id.crypto_info_icon_1_2);
        icon_1_3 = (ImageView) icon1frame.findViewById(R.id.crypto_info_icon_1_3);
        text1 = (TextView) dialogView.findViewById(R.id.crypto_info_text_1);

        icon2frame = dialogView.findViewById(R.id.crypto_info_frame_2);
        icon_2_1 = (ImageView) icon2frame.findViewById(R.id.crypto_info_icon_2_1);
        icon_2_2 = (ImageView) icon2frame.findViewById(R.id.crypto_info_icon_2_2);
        text2 = (TextView) dialogView.findViewById(R.id.crypto_info_text_2);

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

        return b.create();
    }

    private void setMessageForDisplayStatus(MessageCryptoDisplayStatus displayStatus) {

        if (displayStatus.textResSecond == null) {
            setMessageSingleLine(displayStatus.color, displayStatus.textResFirst, displayStatus.iconResFirst, displayStatus.iconResSecond);
        } else {
            if (displayStatus.iconResSecond == null) {
                throw new AssertionError("second icon must be non-null if second text is non-null!");
            }
            setMessageWithAnimation(displayStatus.color,
                    displayStatus.textResFirst, displayStatus.iconResFirst,
                    displayStatus.textResSecond, displayStatus.iconResSecond);
        }
    }

    private void setMessageSingleLine(@ColorRes int colorres,
            @StringRes int text1res, @DrawableRes int icon1res,
            @DrawableRes Integer icon2res) {
        @ColorInt int color = getResources().getColor(colorres);

        icon_1_1.setImageResource(icon1res);
        icon_1_1.setColorFilter(color);
        text1.setText(text1res);

        if (icon2res != null) {
            icon_1_3.setImageResource(icon2res);
            icon_1_3.setColorFilter(color);
            icon_1_3.setVisibility(View.VISIBLE);
        } else {
            icon_1_3.setVisibility(View.GONE);
        }

        text2.setVisibility(View.GONE);
        icon2frame.setVisibility(View.GONE);
    }

    private void setMessageWithAnimation(@ColorRes int colorres,
            @StringRes int text1res, @DrawableRes int icon1res, @StringRes int text2res, @DrawableRes int icon2res) {
        icon_1_1.setImageResource(icon1res);
        icon_1_2.setImageResource(icon2res);
        icon_1_3.setVisibility(View.GONE);
        text1.setText(text1res);

        icon_2_1.setImageResource(icon1res);
        icon_2_2.setImageResource(icon2res);
        text2.setText(text2res);

        icon_1_1.setColorFilter(getResources().getColor(colorres));
        icon_2_2.setColorFilter(getResources().getColor(colorres));

        prepareIconAnimation();
    }

    private void prepareIconAnimation() {
        text1.setAlpha(0.0f);
        text2.setAlpha(0.0f);

        dialogView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                float halfVerticalPixelDifference = (icon2frame.getY() - icon1frame.getY()) / 2.0f;
                icon1frame.setTranslationY(halfVerticalPixelDifference);
                icon2frame.setTranslationY(-halfVerticalPixelDifference);

                icon1frame.animate().translationY(0)
                        .setStartDelay(400)
                        .setDuration(350)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                icon2frame.animate().translationY(0)
                        .setStartDelay(400)
                        .setDuration(350)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                text1.animate().alpha(1.0f).setStartDelay(750).start();
                text2.animate().alpha(1.0f).setStartDelay(750).start();

                view.removeOnLayoutChangeListener(this);
            }
        });
    }
}
