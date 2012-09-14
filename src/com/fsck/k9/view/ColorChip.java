package com.fsck.k9.view;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;

public class ColorChip {
    private static final Path CHIP_PATH = new Path();

    static {

        CHIP_PATH.addCircle(8,8,7f,Path.Direction.CW);
        CHIP_PATH.close();
    }


    private ShapeDrawable mDrawable;

    public ColorChip(int color, boolean messageRead) {

        mDrawable = new ShapeDrawable(new PathShape(CHIP_PATH, 16f, 16f));
        mDrawable.getPaint().setStrokeWidth(2);
        if (messageRead) {
            // Read messages get an outlined circle
            mDrawable.getPaint().setStyle(Paint.Style.STROKE);
        }
        mDrawable.getPaint().setColor(color);


    }

    public ShapeDrawable drawable() {
        return mDrawable;
    }


}
