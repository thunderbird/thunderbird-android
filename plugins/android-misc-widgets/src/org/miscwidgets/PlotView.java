package org.miscwidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class PlotView extends View {

	private Path path;
	private Paint pathPaint;
	private Paint linePaint;
	private float minY;
	private float maxY;

	public PlotView(Context context, AttributeSet attrs) {
		super(context, attrs);
		path = new Path();
		pathPaint = new Paint();
		pathPaint.setColor(0xffdddddd);
		pathPaint.setStyle(Style.STROKE);
		pathPaint.setStrokeWidth(0.01f);
		pathPaint.setAntiAlias(true);
		linePaint = new Paint(pathPaint);
		linePaint.setStrokeWidth(0.003f);
		linePaint.setColor(0xff00dd00);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(0x00000000);
		if (!path.isEmpty()) {
			canvas.scale(getWidth(), getHeight() / (maxY - minY + 0.02f));
			canvas.translate(0, -minY + 0.01f);
			canvas.drawLine(0, 0, 1, 0, linePaint);
			canvas.drawLine(0, 1, 1, 1, linePaint);
			canvas.drawLine(0, maxY, 1, maxY, linePaint);
			canvas.drawLine(0, minY, 1, minY, linePaint);
			canvas.drawPath(path, pathPaint);
		}
	}

	public void addPoint(float x, float y) {
		y = 1 - y;
		if (path.isEmpty()) {
			path.moveTo(x, y);
		}
		path.lineTo(x, y);
		minY = Math.min(minY, y);
		maxY = Math.max(maxY, y);
	}

	public void resetPath() {
		path.reset();
		minY = Float.POSITIVE_INFINITY;
		maxY = Float.NEGATIVE_INFINITY;
	}
}
