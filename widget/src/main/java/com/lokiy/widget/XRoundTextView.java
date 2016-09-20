/**
 * Copyright (C) 2016 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 　　　　http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lokiy.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;

import com.lokiy.utils.UIUtils;

import java.math.BigDecimal;

/**
 * XRoundTextView
 * Created by Luki on 2016/3/5.
 * Version:1
 */
public class XRoundTextView extends XTextView {
	private final int mStrokeBackground;
	private int mStrokeWidth;
	private boolean mPressedSelectorEnable;
	private Paint mPaint;
	private Paint mPaintPressed;
	private Paint mPaintDisable;
	private RectF oval1;
	private Style mStyle = Style.STROKE;
	private boolean requestChange;
	private boolean isMeasureWidth;

	public XRoundTextView(Context context) {
		this(context, null);
	}

	public XRoundTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public XRoundTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		if (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0 && getPaddingBottom() == 0) {
			int top = UIUtils.dp2px(context, 2);
			int left = UIUtils.dp2px(context, 10);
			setPadding(left, top, left, top);
		}
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XRoundTextView);
		int ordinal = a.getInt(R.styleable.XRoundTextView_style, Style.STROKE.ordinal());
		for (Style style : Style.values()) {
			if (style.ordinal() == ordinal) {
				mStyle = style;
				break;
			}
		}
		int mPaintColor = a.getColor(R.styleable.XRoundTextView_paintColor, 0xFFFF4040);
		int mPaintPressedColor = a.getColor(R.styleable.XRoundTextView_paintPressedColor, 0xFFEA2323);
		mStrokeBackground = a.getColor(R.styleable.XRoundTextView_strokeBackground, 0xFFFFFFFF);
		mPressedSelectorEnable = a.getBoolean(R.styleable.XRoundTextView_pressedSelectorEnable, false);
		mStrokeWidth = a.getDimensionPixelOffset(R.styleable.XRoundTextView_strokeWidth, 1);
		a.recycle();
		mPaint = new Paint();
		mPaint.setColor(mPaintColor);
		mPaint.setStrokeWidth(mStrokeWidth);
		mPaint.setAntiAlias(true); // 消除锯齿
		mPaint.setStyle(Paint.Style.STROKE);
		mPaintPressed = new Paint();
		mPaintPressed.setColor(mPaintPressedColor);
		mPaintPressed.setStrokeWidth(mStrokeWidth);
		mPaintPressed.setAntiAlias(true); // 消除锯齿
		mPaintPressed.setStyle(Paint.Style.STROKE);
		mPaintDisable = new Paint();
		mPaintDisable.setColor(0xFFBDBDBD);
		mPaintDisable.setStrokeWidth(mStrokeWidth);
		mPaintDisable.setAntiAlias(true); // 消除锯齿
		mPaintDisable.setStyle(Paint.Style.STROKE);
		oval1 = new RectF();
		setBackgroundResource(android.R.color.transparent);
		setGravity(Gravity.CENTER);
	}

	public void setStrokeWidth(int strokeWidth) {
		this.mStrokeWidth = strokeWidth;
		mPaint.setStrokeWidth(mStrokeWidth);
		mPaintPressed.setStrokeWidth(mStrokeWidth);
		mPaintDisable.setStrokeWidth(mStrokeWidth);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (changed || requestChange) {
			requestChange = false;
			if (Math.abs(left - right) > 2 && !TextUtils.isEmpty(getText())) {
				drawBackground();
			}
		}
	}

	private void drawBackground() {
		if (getWidth() == 0 || getHeight() == 0) {
			return;
		}
		Drawable drawable;
		StateListDrawable stateListDrawable = new StateListDrawable();
		if (mPressedSelectorEnable) {
			stateListDrawable.addState(new int[]{
					android.R.attr.state_pressed
			}, new BitmapDrawable(getResources(), getBitmap(mPaintPressed)));
		}
		stateListDrawable.addState(new int[]{
				-android.R.attr.state_enabled,
				}, new BitmapDrawable(getResources(), getBitmap(mPaintDisable)));
		stateListDrawable.addState(new int[]{}, new BitmapDrawable(getResources(), getBitmap(mPaint)));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mPressedSelectorEnable) {
			drawable = new RippleDrawable(ColorStateList.valueOf(0xFFBDBDBD), stateListDrawable, null);
		} else {
			drawable = stateListDrawable;
		}
		//noinspection deprecation
		setBackgroundDrawable(drawable);
	}

	@NonNull
	private Bitmap getBitmap(Paint paint) {
		int width = getWidth();
		int height = getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		int r = height / 2;
		switch (mStyle) {
			case FILL:
				paint.setStrokeWidth(0);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				float strokeWidth = paint.getStrokeWidth();

				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半圆

				canvas.drawRect(r, 0, width - r, height, paint);

				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半圆
				break;
			case FILL_LEFT_CIRCLE:
				paint.setStrokeWidth(0);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				strokeWidth = paint.getStrokeWidth();

				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半圆

				canvas.drawRect(r, 0, width, height, paint);
				break;
			case FILL_RIGHT_CIRCLE:
				paint.setStrokeWidth(0);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				strokeWidth = paint.getStrokeWidth();
				canvas.drawRect(0, 0, width - r, height, paint);

				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半圆
				break;
			case STROKE:
				paint.setStrokeWidth(mStrokeWidth);
				paint.setStyle(Paint.Style.STROKE);
				strokeWidth = paint.getStrokeWidth() / 2;
				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半弧

				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半弧

				canvas.drawLine(r, 0 + strokeWidth, width - r, 0 + strokeWidth, paint);//上面线
				canvas.drawLine(r, height - strokeWidth, width - r, height - strokeWidth, paint);//下面线
				break;
			case STROKE_WITH_BACKGROUND:
				paint.setStrokeWidth(0);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				int color = paint.getColor();
				paint.setColor(mStrokeBackground);
				strokeWidth = paint.getStrokeWidth();

				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半圆

				canvas.drawRect(r, 0, width - r, height, paint);

				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半圆

				paint.setStrokeWidth(mStrokeWidth);
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(color);
				strokeWidth = paint.getStrokeWidth() / 2;
				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半弧

				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半弧

				canvas.drawLine(r, 0 + strokeWidth, width - r, 0 + strokeWidth, paint);//上面线
				canvas.drawLine(r, height - strokeWidth, width - r, height - strokeWidth, paint);//下面线
				break;
			case STROKE_LEFT_CIRCLE:
				paint.setStrokeWidth(mStrokeWidth);
				paint.setStyle(Paint.Style.STROKE);
				strokeWidth = paint.getStrokeWidth() / 2;
				oval1.set(0 + strokeWidth, 0 + strokeWidth, 2 * r - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 90, 180, false, paint);//左半弧

				canvas.drawLine(r, 0 + strokeWidth, width, 0 + strokeWidth, paint);//上面线
				canvas.drawLine(r, height - strokeWidth, width, height - strokeWidth, paint);//下面线
				canvas.drawLine(width - strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth, paint);//竖线
				break;
			case STROKE_RIGHT_CIRCLE:
				paint.setStrokeWidth(mStrokeWidth);
				paint.setStyle(Paint.Style.STROKE);
				strokeWidth = paint.getStrokeWidth() / 2;
				oval1.set(width - height + strokeWidth, 0 + strokeWidth, width - strokeWidth, height - strokeWidth);
				canvas.drawArc(oval1, 270, 180, false, paint);//右半弧

				canvas.drawLine(0, 0 + strokeWidth, width - r, 0 + strokeWidth, paint);//上面线
				canvas.drawLine(0, height - strokeWidth, width - r, height - strokeWidth, paint);//下面线
				canvas.drawLine(0 + strokeWidth, 0 + strokeWidth, 0 + strokeWidth, height - strokeWidth, paint);//竖线
				break;
			case NONE:
			default:
				break;
		}

		canvas.save();
		canvas.restore();
		return bitmap;
	}
//
//	public void setPressedSelectorEnable(boolean pressedSelectorEnable) {
//		this.mPressedSelectorEnable = pressedSelectorEnable;
//	}

	public void setPaintColor(int paintColor, int paintColorPressed) {
		if (mPaint.getColor() == paintColor && paintColorPressed == mPaintPressed.getColor()) {
			return;
		}
		mPaint.setColor(paintColor);
		mPaintPressed.setColor(paintColorPressed);
		drawBackground();
	}

	public void setMeasureWidth(boolean measureWidth) {
		isMeasureWidth = measureWidth;
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, type);
		if (text != null && getLayoutParams() != null && isMeasureWidth) {
			float width = getPaint().measureText(text.toString());
			BigDecimal bigDecimal = new BigDecimal(String.valueOf(width + getPaddingLeft() + getPaddingRight())).setScale(0, BigDecimal.ROUND_UP);
			getLayoutParams().width = bigDecimal.intValue();
		}
	}

	public Style getStyle() {
		return mStyle;
	}

	public void setStyle(Style style) {
		this.mStyle = style;
		requestChange = true;
		requestLayout();
	}

	public enum Style {
		FILL,
		FILL_RIGHT_CIRCLE,
		FILL_LEFT_CIRCLE,
		STROKE,
		STROKE_LEFT_CIRCLE,
		STROKE_RIGHT_CIRCLE,
		STROKE_WITH_BACKGROUND,
		NONE
	}

}
