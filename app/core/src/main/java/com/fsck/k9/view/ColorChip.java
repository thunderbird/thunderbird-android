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
    public static final Path RIGHT_NOTCH = new Path();
    public static final Path STAR = new Path();

    static {
        CIRCULAR.addCircle(160, 160, 70f, Path.Direction.CW);
        CIRCULAR.close();

        RIGHT_POINTING.addArc(new RectF(80f, 80f, 240f, 240f), 90, 180);
        RIGHT_POINTING.arcTo(new RectF(160f, 0f, 320f, 160f), 180, -90);
        RIGHT_POINTING.arcTo(new RectF(160f, 160f, 320f, 320f), 270, -90);
        RIGHT_POINTING.close();

        RIGHT_NOTCH.addArc(new RectF(80f, 80f, 240f, 240f), 90, 180);
        RIGHT_NOTCH.arcTo(new RectF(160f, 0f, 320f, 160f), 180, -90);
        RIGHT_NOTCH.arcTo(new RectF(160f, 160f, 320f, 320f), 270, -90);
        RIGHT_NOTCH.close();

        LEFT_POINTING.addArc(new RectF(80f, 80f, 240f, 240f), 90, -180);
        LEFT_POINTING.arcTo(new RectF(00f, 00f, 160f, 160f), 0, 90);
        LEFT_POINTING.arcTo(new RectF(00f, 160f, 160f, 320f), 270, 90);
        LEFT_POINTING.close();

        STAR.moveTo(140f, 60f);
        STAR.lineTo(170f, 110f);
        STAR.lineTo(220f, 120f);
        STAR.lineTo(180f, 160f);
        STAR.lineTo(200f, 220f);
        STAR.lineTo(140f, 190f);
        STAR.lineTo(80f, 220f);
        STAR.lineTo(100f, 160f);
        STAR.lineTo(60f, 120f);
        STAR.lineTo(110f, 110f);
        STAR.lineTo(140f, 60f);
        STAR.close();
    }


    private ShapeDrawable mDrawable;

    public ColorChip(int color, boolean messageRead, Path shape) {
        if (shape.equals(STAR)) {
            mDrawable = new ShapeDrawable(new PathShape(shape, 280f, 280f));
        } else {
            mDrawable = new ShapeDrawable(new PathShape(shape, 320f, 320f));
        }

        if (messageRead) {
            // Read messages get an outlined circle
            mDrawable.getPaint().setStyle(Paint.Style.STROKE);
        } else {
            // Unread messages get filled, while retaining the outline, so that they stay the same size
            mDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        }

        mDrawable.getPaint().setStrokeWidth(20);
        mDrawable.getPaint().setColor(color);
    }

    public ShapeDrawable drawable() {
        return mDrawable;
    }
}
