package com.fsck.k9.view;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;

public class ColorChip {
    public static final Path CIRCULAR = new Path();
    public static final Path LEFT_POINTING = new Path();
    public static final Path RIGHT_POINTING = new Path();
    public static final Path STAR = new Path();
    public static final Path CHECKMARK = new Path();


    static {

        CIRCULAR.addCircle(8,8,7f,Path.Direction.CW);
        CIRCULAR.close();

        RIGHT_POINTING.addArc(new RectF(0f,0f,15f,15f) , 90, 180);
        RIGHT_POINTING.lineTo(16f,7f);
        RIGHT_POINTING.lineTo(8f, 15f);
        RIGHT_POINTING.close();

        LEFT_POINTING.addArc(new RectF(0f,0f,15f,15f) , 270, 180);
        LEFT_POINTING.moveTo(8f, 0f);
        LEFT_POINTING.lineTo(0f,7f);
        LEFT_POINTING.lineTo(8f, 15f);
        LEFT_POINTING.close();

        STAR.moveTo(8f,0f);
        STAR.lineTo(11f,5f);
        STAR.lineTo(16f,6f);
        STAR.lineTo(12f,10f);
        STAR.lineTo(14f,16f);
        STAR.lineTo(8f,13f);
        STAR.lineTo(2f,16f);
        STAR.lineTo(4f,10f);
        STAR.lineTo(0f,6f);
        STAR.lineTo(5f,5f);
        STAR.lineTo(8f,0f);
        STAR.close();

        CHECKMARK.moveTo(0f,10f);
        CHECKMARK.lineTo(6f,16f);
        CHECKMARK.moveTo(6f,15f);
        CHECKMARK.lineTo(16f,2f);
    }

    private ShapeDrawable mDrawable;


    public ColorChip(int color, boolean messageRead, Path shape) {

        mDrawable = new ShapeDrawable(new PathShape(shape, 16f, 16f));

        if (shape.equals(CHECKMARK)) {
            mDrawable.getPaint().setStrokeWidth(3);
        } else {
            mDrawable.getPaint().setStrokeWidth(1);
        }
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
