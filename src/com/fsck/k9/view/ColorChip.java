package com.fsck.k9.view;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;

public class ColorChip {
    private static final Path CIRCULAR_CHIP_PATH = new Path();
    private static final Path LEFT_POINTING_CHIP_PATH = new Path();
    private static final Path RIGHT_POINTING_CHIP_PATH = new Path();
    private static final Path STAR_CHIP_PATH = new Path();

    static {

        CIRCULAR_CHIP_PATH.addCircle(8,8,7f,Path.Direction.CW);
        CIRCULAR_CHIP_PATH.close();

        RIGHT_POINTING_CHIP_PATH.addArc(new RectF(0f,0f,15f,15f) , 90, 180);
        RIGHT_POINTING_CHIP_PATH.lineTo(16f,7f);
        RIGHT_POINTING_CHIP_PATH.lineTo(8f, 15f);
        RIGHT_POINTING_CHIP_PATH.close();

        LEFT_POINTING_CHIP_PATH.addArc(new RectF(0f,0f,15f,15f) , 270, 180);
        LEFT_POINTING_CHIP_PATH.moveTo(8f, 0f);
        LEFT_POINTING_CHIP_PATH.lineTo(0f,7f);
        LEFT_POINTING_CHIP_PATH.lineTo(8f, 15f);
        LEFT_POINTING_CHIP_PATH.close();

        STAR_CHIP_PATH.moveTo(8f,0f);
        STAR_CHIP_PATH.lineTo(11f,5f);
        STAR_CHIP_PATH.lineTo(16f,6f);
        STAR_CHIP_PATH.lineTo(12f,10f);
        STAR_CHIP_PATH.lineTo(14f,16f);
        STAR_CHIP_PATH.lineTo(8f,13f);
        STAR_CHIP_PATH.lineTo(2f,16f);
        STAR_CHIP_PATH.lineTo(4f,10f);
        STAR_CHIP_PATH.lineTo(0f,6f);
        STAR_CHIP_PATH.lineTo(5f,5f);
        STAR_CHIP_PATH.lineTo(8f,0f);
        STAR_CHIP_PATH.close();

    }

    private ShapeDrawable mDrawable;

    public ColorChip(int color, boolean messageRead, boolean toMe, boolean fromMe, boolean messageFlagged ) {

        if (messageFlagged) {
            mDrawable = new ShapeDrawable(new PathShape(STAR_CHIP_PATH, 16f, 16f));
        } else if ( fromMe ) {
            mDrawable = new ShapeDrawable(new PathShape(LEFT_POINTING_CHIP_PATH, 16f, 16f));
        } else if ( toMe) {
            mDrawable = new ShapeDrawable(new PathShape(RIGHT_POINTING_CHIP_PATH, 16f, 16f));
        } else {
            mDrawable = new ShapeDrawable(new PathShape(CIRCULAR_CHIP_PATH, 16f, 16f));
        }

        mDrawable.getPaint().setStrokeWidth(1);
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
