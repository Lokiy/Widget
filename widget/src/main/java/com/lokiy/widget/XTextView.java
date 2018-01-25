/*
 *
 * Copyright (C) 2016 Lokiy(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  　　　　http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.lokiy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * XTextView
 * Created by Lokiy on 2015/8/21.
 * Version:1
 */
public class XTextView extends AppCompatTextView {

	private int minTextSize;
	private int maxTextSize;
	private boolean autoZoom;
	private Paint textPaint;
	private CharSequence mLastMeasureText;

	public XTextView(Context context) {
		this(context, null);
	}

	public XTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public XTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.XTextView);
		String debugText = a.getString(R.styleable.XTextView_debugText);
		autoZoom = a.getBoolean(R.styleable.XTextView_autoZoom, false);
		this.minTextSize = a.getDimensionPixelSize(R.styleable.XTextView_minTextSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9, context.getResources().getDisplayMetrics()));
		this.maxTextSize = a.getDimensionPixelSize(R.styleable.XTextView_maxTextSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 60, context.getResources().getDisplayMetrics()));

		a.recycle();
		if (autoZoom) {
			this.textPaint = new Paint();
			setSingleLine();
		}
		if (isInEditMode()) {
			if (!TextUtils.isEmpty(debugText)) {
				setText(getFormatText(debugText));
			}
		}
	}

	public void setAutoZoom(boolean autoZoom) {
		this.autoZoom = autoZoom;
	}

	public void setMaxTextSize(int maxTextSize) {
		this.maxTextSize = maxTextSize;
	}

	public void setMinTextSize(int minTextSize) {
		this.minTextSize = minTextSize;
	}

	private CharSequence getFormatText(String replace) {
		String[] format = getContext().getResources().getStringArray(R.array.TextFormat);
		String[][] formatArray = new String[format.length][2];
		for (int i = 0; i < format.length; i++) {
			String[] split = format[i].split("[|]");
			if (split.length > 1) {
				formatArray[i][0] = split[0];
				formatArray[i][1] = split[1];
			}
		}
		String result = replace;
		for (String[] aFormatArray : formatArray) {
			result = result.replace(aFormatArray[0], aFormatArray[1]);
		}
		return Html.fromHtml(result);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		if (text instanceof String && ((String) text).contains("s") && ((String) text).contains("e")) {
			text = getFormatText((String) text);
		} else if (text instanceof String && ((String) text).contains("<") && ((String) text).contains(">")) {
			text = Html.fromHtml((String) text);
		}
		super.setText(text, type);
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
		if (autoZoom) {
			this.refitText(text, this.getWidth());
		} else {
			super.onTextChanged(text, start, before, after);
		}
	}

	/**
	 * Resize the text so that it fits
	 *
	 * @param text  The text. Neither <code>null</code> nor empty.
	 * @param width The width of the TextView. > 0
	 */
	private void refitText(CharSequence text, int width) {
		if (width <= 0 || text == null || text.length() == 0 || (mLastMeasureText != null && text.length() == mLastMeasureText.length()))
			return;
		mLastMeasureText = text;
		// the width
		int targetWidth = width - this.getPaddingLeft() - this.getPaddingRight();

		this.textPaint.set(this.getPaint());
		String measureText = text.toString();
		int i = 0;
		while ((textPaint.measureText(measureText)) < targetWidth && i < 200) {//too small
			float size = getTextSize() + 1f;
			if (size > maxTextSize) {
				break;
			}
			this.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
			this.textPaint.setTextSize(size);
			i++;
		}
		i = 0;
		while ((textPaint.measureText(measureText)) > targetWidth && i < 200) {//too big
			float size = getTextSize() - 1f;
			if (size < minTextSize) {
				break;
			}
			this.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
			this.textPaint.setTextSize(size);
			i++;
		}
		i = 0;
		while (textPaint.getTextSize() > getHeight() && getHeight() != 0 && i < 200) {//too big
			float size = getTextSize() - 1f;
			if (size < minTextSize) {
				break;
			}
			this.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
			this.textPaint.setTextSize(size);
			i++;
		}
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		super.setOnClickListener(new ProxyClickListener(l));
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		if (autoZoom) {
			if (width != oldWidth)
				this.refitText(getText(), width);
		} else {
			super.onSizeChanged(width, height, oldWidth, oldHeight);
		}
	}

	private class ProxyClickListener implements OnClickListener {
		private final OnClickListener listener;
		private long lastEnablePressedTime;

		ProxyClickListener(OnClickListener listener) {
			this.listener = listener;
		}

		@Override
		public void onClick(View v) {
			if (System.currentTimeMillis() - lastEnablePressedTime < 300) {
				return;
			}
			lastEnablePressedTime = System.currentTimeMillis();
			if (listener != null) {
				listener.onClick(v);
			}
		}
	}
}