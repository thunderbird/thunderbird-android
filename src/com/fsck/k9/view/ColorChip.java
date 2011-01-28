package com.fsck.k9.view;

import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;

public class ColorChip
{
    private static final Path CHIP_PATH = new Path();

    static
    {

        CHIP_PATH.lineTo(6,0);

        CHIP_PATH.cubicTo( 8f, 0f, 10f, 0f, 10f, 2f );
        CHIP_PATH.lineTo(10, 8);
        CHIP_PATH.cubicTo( 10f, 9f, 10f, 10f, 6f, 10f );
        CHIP_PATH.lineTo(0,10);
        CHIP_PATH.close();
    }


    private ShapeDrawable mDrawable;

    public ColorChip(int color)
    {

        mDrawable = new ShapeDrawable(new PathShape(CHIP_PATH, 10, 10));
        mDrawable.getPaint().setColor(color);


    }

    public ShapeDrawable drawable ()
    {

        return mDrawable;
    }


}
