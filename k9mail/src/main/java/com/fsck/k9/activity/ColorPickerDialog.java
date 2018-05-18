package com.fsck.k9.activity;

import com.fsck.k9.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.view.LayoutInflater;
import android.view.View;

import com.larswerkman.colorpicker.ColorPicker;


/**
 * Dialog displaying a color picker.
 */
public class ColorPickerDialog extends AlertDialog {

    /**
     * The interface users of {@link ColorPickerDialog} have to implement to learn the selected
     * color.
     */
    public interface OnColorChangedListener {
        /**
         * This is called after the user pressed the "OK" button of the dialog.
         *
         * @param color
         *         The ARGB value of the selected color.
         */
        void colorChanged(int color);
    }

    OnColorChangedListener mColorChangedListener;
    ColorPicker mColorPicker;

    public ColorPickerDialog(Context context, OnColorChangedListener listener, int color) {
        super(context);
        mColorChangedListener = listener;

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.holo_color_picker_dialog, null);

        mColorPicker = view.findViewById(R.id.color_picker);
        mColorPicker.setColor(color);

        setView(view);
        setTitle(R.string.color_picker_default_title);

        setButton(BUTTON_POSITIVE, context.getString(R.string.okay_action),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mColorChangedListener != null) {
                    mColorChangedListener.colorChanged(mColorPicker.getColor());
                }
            }
        });

        setButton(BUTTON_NEUTRAL, context.getString(R.string.account_settings_color_none),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mColorChangedListener != null) {
                    mColorChangedListener.colorChanged(0x0); // Transparent
                }
            }
        });

        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel_action),
                (OnClickListener) null);
    }

    /**
     * Set the color the color picker should highlight as selected color.
     *
     * @param color
     *         The (A)RGB value of a color (the alpha channel will be ignored).
     */
    public void setColor(int color) {
        mColorPicker.setColor(color);
    }
}
