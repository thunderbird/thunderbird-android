/* Sourced from http://code.google.com/p/android-color-picker/source/browse/trunk/AmbilWarna/src/yuku/ambilwarna/AmbilWarnaBox.java?r=1
 * On 2010-11-07
 * Translated to English, Ported to use the same (inferior) API as the more standard "ColorPickerDialog" and imported into the K-9 namespace by Jesse Vincent
 * In an ideal world, we should move to using AmbilWarna as an Android Library Project in the future
 * License: Apache 2.0
 * Author: yukuku@code.google.com
 */



package com.fsck.k9.activity;
import com.fsck.k9.R;
import android.content.*;
import android.graphics.*;
import android.graphics.Shader.*;
import android.util.*;
import android.view.*;

public class ColorPickerBox extends View
{

    Paint paint;
    Shader dalam;
    Shader luar;
    float hue;
    float onedp;
    float sizeUiDp = 240.f;
    float sizeUiPx; // diset di constructor
    float[] tmp00 = new float[3];

    public ColorPickerBox(Context context)
    {
        this(context, null);
    }

    public ColorPickerBox(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ColorPickerBox(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        onedp = context.getResources().getDimension(R.dimen.colorpicker_onedp);
        sizeUiPx = sizeUiDp * onedp;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (paint == null)
        {
            paint = new Paint();
            luar = new LinearGradient(0.f, 0.f, 0.f, sizeUiPx, 0xffffffff, 0xff000000, TileMode.CLAMP);
        }

        tmp00[1] = tmp00[2] = 1.f;
        tmp00[0] = hue;
        int rgb = Color.HSVToColor(tmp00);

        dalam = new LinearGradient(0.f, 0.f, sizeUiPx, 0.f, 0xffffffff, rgb, TileMode.CLAMP);
        ComposeShader shader = new ComposeShader(luar, dalam, PorterDuff.Mode.MULTIPLY);

        paint.setShader(shader);

        canvas.drawRect(0.f, 0.f, sizeUiPx, sizeUiPx, paint);
    }

    void setHue(float hue)
    {
        this.hue = hue;
        invalidate();
    }
}
