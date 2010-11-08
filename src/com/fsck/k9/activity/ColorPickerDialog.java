/* Sourced from http://code.google.com/p/android-color-picker/source/browse/trunk/AmbilWarna/src/yuku/ambilwarna/AmbilWarnaDialog.java?r=1
 * On 2010-11-07 
 * Translated to English, Ported to use the same (inferior) API as the more standard "ColorPickerDialog" and imported into the K-9 namespace by Jesse Vincent
 * In an ideal world, we should move to using AmbilWarna as an Android Library Project in the future
 * License: Apache 2.0 
 * Author: yukuku@code.google.com
 */


package com.fsck.k9.activity;
import com.fsck.k9.R;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class ColorPickerDialog
{
    private static final String TAG = ColorPickerDialog.class.getSimpleName();

    public interface OnColorChangedListener
    {
        void colorChanged(int color);
    }

    AlertDialog dialog;
    OnColorChangedListener listener;
    View viewHue;
    ColorPickerBox viewBox;
    ImageView arrow;
    View viewColorOld;
    View viewColorNew;
    ImageView viewSpyglass;

    float onedp;
    int colorOld;
    int colorNew;
    float hue;
    float sat;
    float val;
    float sizeUiDp = 240.f;
    float sizeUiPx; // diset di constructor

    public ColorPickerDialog(Context context, OnColorChangedListener listener, int color )
    {
        this.listener = listener;
        this.colorOld = color;
        this.colorNew = color;
        Color.colorToHSV(color, tmp01);
        hue = tmp01[0];
        sat = tmp01[1];
        val = tmp01[2];

        onedp = context.getResources().getDimension(R.dimen.colorpicker_onedp);
        sizeUiPx = sizeUiDp * onedp;
        Log.d(TAG, "onedp = " + onedp + ", sizeUiPx=" + sizeUiPx);  //$NON-NLS-1$//$NON-NLS-2$

        View view = LayoutInflater.from(context).inflate(R.layout.colorpicker_dialog, null);
        viewHue = view.findViewById(R.id.colorpicker_viewHue);
        viewBox = (ColorPickerBox) view.findViewById(R.id.colorpicker_viewBox);
        arrow = (ImageView) view.findViewById(R.id.colorpicker_arrow);
        viewColorOld = view.findViewById(R.id.colorpicker_colorOld);
        viewColorNew = view.findViewById(R.id.colorpicker_colorNew);
        viewSpyglass = (ImageView) view.findViewById(R.id.colorpicker_spyglass);

        placeArrow();
        placeSpyglass();
        viewBox.setHue(hue);
        viewColorOld.setBackgroundColor(color);
        viewColorNew.setBackgroundColor(color);

        viewHue.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP)
                {

                    float y = event.getY(); // dalam px, bukan dp
                    if (y < 0.f) y = 0.f;
                    if (y > sizeUiPx) y = sizeUiPx - 0.001f;

                    hue = 360.f - 360.f / sizeUiPx * y;
                    if (hue == 360.f) hue = 0.f;

                    colorNew = calculateColor();
                    // update view
                    viewBox.setHue(hue);
                    placeArrow();
                    viewColorNew.setBackgroundColor(colorNew);

                    return true;
                }
                return false;
            }
        });
        viewBox.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP)
                {

                    float x = event.getX(); // dalam px, bukan dp
                    float y = event.getY(); // dalam px, bukan dp

                    if (x < 0.f) x = 0.f;
                    if (x > sizeUiPx) x = sizeUiPx;
                    if (y < 0.f) y = 0.f;
                    if (y > sizeUiPx) y = sizeUiPx;

                    sat = (1.f / sizeUiPx * x);
                    val = 1.f - (1.f / sizeUiPx * y);

                    colorNew = calculateColor();
                    // update view
                    placeSpyglass();
                    viewColorNew.setBackgroundColor(colorNew);

                    return true;
                }
                return false;
            }
        });

        dialog = new AlertDialog.Builder(context)
        .setView(view)
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (ColorPickerDialog.this.listener != null)
                {
                    ColorPickerDialog.this.listener.colorChanged(colorNew);
                }
            }
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (ColorPickerDialog.this.listener != null)
                {
                }
            }
        })
        .create();

    }

    @SuppressWarnings("deprecation")
    protected void placeArrow()
    {
        float y = sizeUiPx - (hue * sizeUiPx / 360.f);
        if (y == sizeUiPx) y = 0.f;

        AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) arrow.getLayoutParams();
        layoutParams.y = (int) (y + 4);
        arrow.setLayoutParams(layoutParams);
    }

    @SuppressWarnings("deprecation")
    protected void placeSpyglass()
    {
        float x = sat * sizeUiPx;
        float y = (1.f - val) * sizeUiPx;

        AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) viewSpyglass.getLayoutParams();
        layoutParams.x = (int) (x + 3);
        layoutParams.y = (int) (y + 3);
        viewSpyglass.setLayoutParams(layoutParams);
    }

    float[] tmp01 = new float[3];
    private int calculateColor()
    {
        tmp01[0] = hue;
        tmp01[1] = sat;
        tmp01[2] = val;
        return Color.HSVToColor(tmp01);
    }

    public void show()
    {
        dialog.show();
    }
}
