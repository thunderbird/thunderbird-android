package org.miscwidgets.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.miscwidgets.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

public class VirtualKeyboard extends View {

	private static final int ZOOM_RADIUS = 58;
	private static final int ZOOM_SIZE = 131;
	private static final int HANDLE_SIZE = 33;
	private static final long ZOOM_DELAY_TIME = 250;
	private static final int BUTTON_SIZE = 64;
	private Bitmap kbdBitmap;
	private ButtonDrawable btnDrawable;
	private ButtonDrawable btnCtrlDrawable;
	private ButtonDrawable btnSelectedDrawable;
	private ButtonDrawable btnToggleSelectedDrawable;
	private List<VirtualButton> buttons;
	private Paint letterPaint;
	private Rect srcRect;
	private Point dstPoint;
	private Bitmap zoomBitmap;
	private VirtualButton pressedButton;
	public Paint backgroundPaint;
	private int state;
	private PopupWindow zoom;
	private ZoomView zoomView;
	private Bitmap handleBitmap;
	private ToggleVirtualButton shiftBtn;
	private VirtualButton enterBtn;
	private VirtualButton symbolsBtn;
	private boolean zoomVisible;
	private Canvas kbdCanvas;

	public VirtualKeyboard(Context context, AttributeSet attrs) {
		super(context, attrs);
		Resources res = context.getResources();
		btnDrawable = new ButtonDrawable(res, R.drawable.vk_button);
		btnCtrlDrawable = new ButtonDrawable(res, R.drawable.vk_button_ctrl);
		btnSelectedDrawable = new ButtonDrawable(res, R.drawable.vk_button_selected);
		btnToggleSelectedDrawable = new ButtonDrawable(res, R.drawable.vk_button_toggle_selected);

		BitmapDrawable zoomDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.vk_zoom);
		zoomBitmap = zoomDrawable.getBitmap();
		BitmapDrawable handleDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.vk_zoom_handle);
		handleBitmap = handleDrawable.getBitmap();

		buttons = new ArrayList<VirtualButton>();
		letterPaint = new Paint();
		letterPaint.setAntiAlias(true);
		letterPaint.setColor(0xff000000);
		letterPaint.setTypeface(Typeface.DEFAULT_BOLD);
		letterPaint.setTextSize(40);
		backgroundPaint = new Paint();
		backgroundPaint.setColor(0xffffffff);
		backgroundPaint.setStyle(Style.FILL);

		srcRect = new Rect(0, 0, 2 * ZOOM_RADIUS, 2 * ZOOM_RADIUS);
		dstPoint = new Point(0, 0);

		pressedButton = null;
		state = 0;

		zoomView = new ZoomView(getContext());
		zoom = new PopupWindow(zoomView, ZOOM_SIZE, ZOOM_SIZE);
		zoom.setAnimationStyle(android.R.style.Animation_Toast);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		createBitmap(2 * w + BUTTON_SIZE, 2 * h + BUTTON_SIZE);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private void createBitmap(int w, int h) {
		kbdBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		kbdCanvas = new Canvas(kbdBitmap);
		Resources res = getResources();
		VirtualButton vButton;

		String packageName = R.class.getPackage().getName();
		XmlPullParser xpp = getResources().getXml(R.xml.layout);
		try {
			while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
				if (xpp.getEventType() == XmlPullParser.START_TAG) {
					String name = xpp.getName();
					if (!name.equals("Layout")) {
						String sX = xpp.getAttributeValue(null, "x");
						float x = 0;
						if (sX != null) {
							x = Float.parseFloat(sX);
						}
						String sY = xpp.getAttributeValue(null, "y");
						float y = 0;
						if (sY != null) {
							y = Float.parseFloat(sY);
						}
						String sWidth = xpp.getAttributeValue(null, "width");
						float width = 1f;
						if (sWidth != null) {
							width = Float.parseFloat(sWidth);
						}
						String sText = xpp.getAttributeValue(null, "text");
						String sDrawable = xpp.getAttributeValue(null, "drawable");
						if (name.equals("ShiftButton")) {
							vButton = shiftBtn = new ToggleVirtualButton(x, y, width, "");
						} else
						if (name.equals("BackspaceButton")) {
							vButton = new VirtualButton(x, y, width, "\b\b\b\b");
						} else
						if (name.equals("SymbolsButton")) {
							vButton = symbolsBtn = new ToggleVirtualButton(x, y, width, sText);
						} else
						if (name.equals("EnterButton")) {
							vButton = enterBtn = new ControlVirtualButton(x, y, width, sText);
						} else {
							if (sText.length() < 4) {
								Log.d("VirtualKeyboard", " Button x:" + x + " y:" + y + " has text shorter than 4 characters !!");
								sText = "????";
							}
							vButton = new VirtualButton(x, y, width, sText);
						}
						if (sDrawable != null) {
							int id = res.getIdentifier(sDrawable, null, packageName);
							if (id == 0) {
								throw new RuntimeException("resource: " + sDrawable + " not found");
							}
							vButton.setBitmap(id);
						}
						buttons.add(vButton);
					}
				}
				xpp.next();
			}
		} catch (XmlPullParserException e) {
			Log.d("VirtualKeyboard", "XmlPullParserException", e);
		} catch (IOException e) {
			Log.d("VirtualKeyboard", "IOException", e);
		}

		drawLayout();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.scale(0.5f, 0.5f);
		canvas.translate(-BUTTON_SIZE/2, -BUTTON_SIZE/2);
		canvas.drawBitmap(kbdBitmap, 0, 0, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			// offset getX & getY by BUTTON_SIZE/4: bitmap padding
			int x = (int) event.getX() + BUTTON_SIZE/4;
			int y = (int) event.getY() + BUTTON_SIZE/4;
			srcRect.offsetTo(2 * x - ZOOM_RADIUS, 2 * y - ZOOM_RADIUS);
			dstPoint.set(x - ZOOM_RADIUS, y - 3 * ZOOM_RADIUS / 2);
			int xx = x * 2;
			int yy = y * 2;
			boolean in = false;
			for (VirtualButton button : buttons) {
				if (button.contains(xx, yy)) {
					in = true;
					if (pressedButton == button) {
						break;
					}
					if (pressedButton != null) {
						pressedButton.up(false);
					}
					pressedButton = button;
					pressedButton.down();
					break;
				}
			}
			if (!in && pressedButton != null) {
				pressedButton.up(false);
				pressedButton = null;
			}
			if (pressedButton != null) {
				srcRect.offsetTo(2 * x - ZOOM_RADIUS, pressedButton.getRect().centerY() - ZOOM_RADIUS);
				dstPoint.set(x - ZOOM_RADIUS, pressedButton.getRect().top/2 - 3 * ZOOM_RADIUS / 2);
			}
			if (srcRect.left < 0) {
				srcRect.offset(-srcRect.left, 0);
			} else
			if (srcRect.right > kbdBitmap.getWidth()) {
				srcRect.offset(kbdBitmap.getWidth() - srcRect.right, 0);
			}
			if (srcRect.top < 0) {
				srcRect.offset(0, -srcRect.top);
			} else
			if (srcRect.bottom > kbdBitmap.getHeight()) {
				srcRect.offset(0, kbdBitmap.getHeight() - srcRect.bottom);
			}
			dstPoint.offset(-BUTTON_SIZE/4, -BUTTON_SIZE/4);

			if (y < BUTTON_SIZE/4) {
				// hide zoom if out of bounds
				zoom.dismiss();
				invalidate();
				return true;
			}
			if (action == MotionEvent.ACTION_DOWN) {
				removeCallbacks(showZoom);
				postDelayed(showZoom, ZOOM_DELAY_TIME);
			} else {
				if (zoomVisible && !zoom.isShowing()) {
					// zoom was hidden since out of bounds (y < 0)
					showZoom.run();
				}
				zoom.update(getLeft() + dstPoint.x, getTop() + dstPoint.y, -1, -1);
			}
			zoomView.invalidate();
		} else
		if (action == MotionEvent.ACTION_UP) {
			if (pressedButton != null) {
				pressedButton.up(true);

				if (pressedButton == shiftBtn) {
					state ^= 1;
					drawLayout();
				} else
				if (pressedButton == symbolsBtn) {
					state ^= 2;
					drawLayout();
				} else
				if (pressedButton == enterBtn) {
					// TODO implement me?
				} else {
					updateEditText(pressedButton.text.charAt(state));
				}
				pressedButton = null;
			}
			pressedButton = null;
			removeCallbacks(showZoom);
			zoomVisible = false;
			zoom.dismiss();
		}
		invalidate();
		return true;
	}

	private void drawLayout() {
//		long t0 = System.currentTimeMillis();
		kbdCanvas.drawARGB(255, 255, 255, 255);
		for (VirtualButton button : buttons) {
			button.draw(kbdCanvas);
		}
//		long t1 = System.currentTimeMillis();
//		Log.d("VirtualKeyboard", "drawLayout took " + (t1-t0) + "ms");
	}

	private void updateEditText(char c) {
		int start = edit.getSelectionStart();
		int end = edit.getSelectionEnd();
		if (start > end) {
			int tmp = end;
			end = start;
			start = tmp;
		}
		Editable text = edit.getText();
		if (c != '\b') {
			text.replace(start, end, String.valueOf(c), 0, 1);
			start++;
		} else {
			if (start == end && start > 0) {
				start--;
			}
			if (start + end == 0) {
				// start of buffer & no selection: just return
				return;
			}
			text.delete(start, end);
		}
		edit.setText(text);
		edit.setSelection(start);
	}

	Runnable showZoom = new Runnable() {
		public void run() {
			zoomVisible = true;
			zoom.showAtLocation(VirtualKeyboard.this,
					Gravity.NO_GRAVITY,
					getLeft() + dstPoint.x,
					getTop() + dstPoint.y);
		}
	};


	// tmp
	private EditText edit;
	public void setUp(EditText edit) {
		this.edit = edit;
	}
	// end of tmp

	class VirtualButton {
		private Rect rect;
		protected String text;
		private Bitmap bitmap;

		public VirtualButton(float x, float y, String letters) {
			this(x, y, 1, letters);
		}
		public VirtualButton(float x, float y, float w, String letters) {
			int left = (int) (x * BUTTON_SIZE);
			int top = (int) (y * BUTTON_SIZE);
			int right = (int) ((x + w) * BUTTON_SIZE);
			int bottom = (int) ((y + 1) * BUTTON_SIZE);
			this.rect = new Rect(left, top, right, bottom);
			this.rect.offset(BUTTON_SIZE/2, BUTTON_SIZE/2);
			this.text = letters;
		}
		public void setBitmap(int resId) {
			BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(resId);
			bitmap = drawable.getBitmap();
		}
		public Rect getRect() {
			return rect;
		}

		public void up(boolean inside) {
			draw(kbdCanvas, btnDrawable, text.substring(state, state+1));
		}

		public void down() {
			draw(kbdCanvas, null, text.substring(state, state+1));
		}

		public boolean contains(int x, int y) {
			return rect.contains(x, y);
		}

		public void draw(Canvas canvas) {
			draw(canvas, btnDrawable, text.substring(state, state+1));
		}

		void draw(Canvas canvas, ButtonDrawable buttonDrawable, String t) {
			canvas.drawRect(rect, backgroundPaint);
			if (buttonDrawable != null) {
				Bitmap bitmap = buttonDrawable.getBitmap(rect);
				canvas.drawBitmap(bitmap, rect.left, rect.top, null);
			} else {
				Bitmap bitmap = btnSelectedDrawable.getBitmap(rect);
				canvas.drawBitmap(bitmap, rect.left, rect.top, null);
			}
			if (bitmap != null) {
				canvas.drawBitmap(bitmap,
						rect.centerX() - bitmap.getWidth() / 2,
						rect.centerY() - bitmap.getHeight() / 2,
						null);
			} else {
				float x = (rect.width() - letterPaint.measureText(t)) / 2;
				FontMetrics fm = letterPaint.getFontMetrics();
				float y = (rect.height() - letterPaint.getTextSize()) / 2 - fm.ascent - fm.descent;
				// TODO fix it
				y += 4;
				canvas.drawText(t, rect.left+x, rect.top+y, letterPaint);
			}
		}
	}

	class ControlVirtualButton extends VirtualButton {
		public ControlVirtualButton(float x, float y, float w, String letters) {
			super(x, y, w, letters);
		}
		public ControlVirtualButton(float x, float y, String letters) {
			super(x, y, letters);
		}
		@Override
		public void draw(Canvas canvas) {
			draw(canvas, btnCtrlDrawable, text);
		}
		public void up(boolean inside) {
			draw(kbdCanvas, btnCtrlDrawable, text);
		}
		public void down() {
			draw(kbdCanvas, null, text);
		}
	}
	class ToggleVirtualButton extends ControlVirtualButton {
		private boolean down;
		public ToggleVirtualButton(float x, float y, float w, String letters) {
			super(x, y, w, letters);
		}
		public ToggleVirtualButton(float x, float y, String letters) {
			super(x, y, letters);
		}
		@Override
		public void draw(Canvas canvas) {
			draw(canvas, down? btnToggleSelectedDrawable : btnCtrlDrawable, text);
		}
		@Override
		public void up(boolean inside) {
			if (inside) {
				down = !down;
			}
			draw(kbdCanvas, down? btnToggleSelectedDrawable : btnCtrlDrawable, text);
		}
	}

	class ZoomView extends View {
		private Paint zoomPaint;
		private Rect rect;
		private Path clip;

		public ZoomView(Context context) {
			super(context);
			zoomPaint = new Paint();
			zoomPaint.setAntiAlias(true);
			zoomPaint.setColor(0xff008000);
			zoomPaint.setStyle(Style.STROKE);
			rect = new Rect(2, 2, 2 + ZOOM_RADIUS * 2, 2 + ZOOM_RADIUS * 2);
			clip = new Path();
			clip.addCircle(ZOOM_RADIUS+2, ZOOM_RADIUS+2, ZOOM_RADIUS, Direction.CW);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.save();
			canvas.clipPath(clip);
			// draw zoom
			zoomPaint.setAlpha(255);
			canvas.drawBitmap(kbdBitmap, srcRect, rect, zoomPaint);
			canvas.restore();
			// draw zoom frame
			zoomPaint.setAlpha(220);
			canvas.drawBitmap(zoomBitmap, 0, 0, zoomPaint);
			// draw zoom handle
			zoomPaint.setAlpha(255);
			canvas.drawBitmap(handleBitmap, ZOOM_SIZE-HANDLE_SIZE, ZOOM_SIZE-HANDLE_SIZE, zoomPaint);
		}
	}

	class ButtonDrawable {
		private Drawable drawable;
		private SparseArray<Bitmap> bitmapCache;
		public ButtonDrawable(Resources res, int id) {
			drawable = res.getDrawable(id);
			bitmapCache = new SparseArray<Bitmap>();
		}
		public Bitmap getBitmap(Rect rect) {
			int w = rect.width();
			int h = rect.height();
			int key = (w << 16) + h;
			Bitmap bitmap = bitmapCache.get(key);
			if (bitmap == null) {
				drawable.setBounds(0, 0, w, h);
				bitmap =  Bitmap.createBitmap(w, h, Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				drawable.draw(canvas);
				bitmapCache.put(key, bitmap);
			}
			return bitmap;
		}
	}
}
