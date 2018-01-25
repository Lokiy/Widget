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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author Luki
 */
@SuppressLint("ClickableViewAccessibility")
public class XViewPager extends ViewPager {
	private static Handler handler = new Handler();
	private boolean enableScrolled = true;
	private boolean isAutoScroll;

	private Runnable action = new Runnable() {

		@Override
		public void run() {
			setCurrentItem(getCurrentItem() + 1, true);
			if (isAutoScroll) {
				startTimer();
			}
		}
	};

	public XViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// 告诉父view，我的事件自己处理(解决ViewPager嵌套引起子ViewPager无法触摸问题)
//		getParent().requestDisallowInterceptTouchEvent(true);
		int action = MotionEventCompat.getActionMasked(ev);
		if (action == MotionEvent.ACTION_DOWN) {
			stopTimer();
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			if (isAutoScroll) {
				startTimer();
			}
		}
		return super.dispatchTouchEvent(ev);
	}

	private void stopTimer() {
		handler.removeCallbacks(action);
	}

	private void startTimer() {
		stopTimer();
		long delay = 5000;
		handler.postDelayed(action, delay);
	}

	public void startScroll() {
		isAutoScroll = true;
		startTimer();
	}

	public void stopScroll() {
		isAutoScroll = false;
		stopTimer();
	}

	public void setPagingEnableScrolled(boolean enabled) {
		this.enableScrolled = enabled;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		pause();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		resume();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.enableScrolled) {
			boolean b = false;
			try {
				b = super.onInterceptTouchEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return b;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (this.enableScrolled) {
			boolean touchEvent = false;
			try {
				touchEvent = super.onTouchEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return touchEvent;
		}

		return false;
	}

	public void resume() {
		if (isAutoScroll) {
			startTimer();
		}
	}

	public void pause() {
		stopTimer();
	}
}
