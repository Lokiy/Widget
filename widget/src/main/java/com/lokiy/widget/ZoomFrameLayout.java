/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lokiy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;


public class ZoomFrameLayout extends FrameLayout {

	private float mZoomSize;
	private List<View> mMatchParentChildren = new ArrayList<>();

	public ZoomFrameLayout(Context context) {
		this(context, null);
	}

	public ZoomFrameLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZoomFrameLayout);
		mZoomSize = a.getFloat(R.styleable.ZoomFrameLayout_zoomSize, 0f);
		a.recycle();
	}

	public float getZoomSize() {
		return mZoomSize;
	}

	/**
	 * @param zoomSize the mZoomSize to set
	 */
	public void setZoomSize(float zoomSize) {
		this.mZoomSize = zoomSize;
	}

	@SuppressWarnings("unused")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mZoomSize != 0 && getVisibility() != GONE) {
			int w;
			int h;
			if (mZoomSize > 0) {
				w = MeasureSpec.getSize(widthMeasureSpec);
				h = (int) (w * mZoomSize);
			} else {
				h = MeasureSpec.getSize(heightMeasureSpec);
				w = (int) (h * mZoomSize * -1);
			}
			setMeasuredDimension(w, h);

			int count = getChildCount();

			final boolean measureMatchParentChildren = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY || MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
			mMatchParentChildren.clear();

			int maxHeight = 0;
			int maxWidth = 0;
			int childState = 0;

			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != GONE) {
					measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
					final LayoutParams lp = (LayoutParams) child.getLayoutParams();
					maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
					maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);

					int childMeasuerdState = (child.getMeasuredWidth() & MEASURED_STATE_MASK) | ((child.getMeasuredHeight() >> MEASURED_HEIGHT_STATE_SHIFT) & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));

					childState = childState | childMeasuerdState;
//					if (measureMatchParentChildren) {
					if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
						mMatchParentChildren.add(child);
					}
//					}
				}
			}

			// Account for padding too
			maxWidth += getPaddingLeft() + getPaddingRight();
			maxHeight += getPaddingTop() + getPaddingBottom();

			// Check against our minimum height and width
			maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
			maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

			// Check against our foreground's minimum height and width
			final Drawable drawable = getForeground();
			if (drawable != null) {
				maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
				maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
			}

			count = mMatchParentChildren.size();
			if (count > 0) {
				for (int i = 0; i < count; i++) {
					final View child = mMatchParentChildren.get(i);

					final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
					int childWidthMeasureSpec;
					int childHeightMeasureSpec;

					if (lp.width == LayoutParams.MATCH_PARENT) {
						childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(w - getPaddingLeft() - getPaddingRight() - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
					} else {
						childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
					}

					if (lp.height == LayoutParams.MATCH_PARENT) {
						childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(h - getPaddingTop() - getPaddingBottom() - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
					} else {
						childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin, lp.height);
					}

					child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
				}
			}
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

}
