/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 　　　http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lokiy.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lokiy.utils.UIUtils;
import com.lokiy.view.XImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luki
 */
public class BannerView extends ZoomFrameLayout {

	public static final ImageLoader INSTANCE = ImageLoader.getInstance();
	private final int mPageMargin;
	private XViewPager mViewPager;
	private BannerAdapter mAdapter;
	private RadioGroup vRadioGroup;
	private boolean isShowIndicator;
	private int mIndicatorId;
	private OnPageSelectedListener onPageSelectedListener;
	private Object host;
	private boolean isCorner;

	public BannerView(Context context) {
		this(context, null);
	}

	public BannerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BannerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BannerView);
		isShowIndicator = a.getBoolean(R.styleable.BannerView_showIndicator, true);
		mIndicatorId = a.getResourceId(R.styleable.BannerView_indicator, 0);
		isCorner = a.getBoolean(R.styleable.BannerView_corner, false);
		mPageMargin = a.getDimensionPixelSize(R.styleable.BannerView_pageMargin, 0);
		int mIndicatorGravity = a.getInt(R.styleable.BannerView_indicator_gravity, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
		a.recycle();
		mViewPager = new XViewPager(getContext(), null);
		mViewPager.setPageMargin(mPageMargin);
		addView(mViewPager, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setClipChildren(false);
		if (isShowIndicator) {
			vRadioGroup = new RadioGroup(getContext());
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.gravity = mIndicatorGravity;
			vRadioGroup.setGravity(Gravity.CENTER);
			vRadioGroup.setOrientation(LinearLayout.HORIZONTAL);
			addView(vRadioGroup, params);
		}
		mAdapter = new BannerAdapter();
		//noinspection deprecation
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int arg0) {
				int position = arg0 % mAdapter.getRealCount();
				if (isShowIndicator)
					vRadioGroup.check(position);
				if (onPageSelectedListener != null) {
					onPageSelectedListener.onPageSelected(position, mAdapter.mBannerList.get(position));
				}
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		mViewPager.setAdapter(mAdapter);
	}

	public void setData(List<? extends IImage> data) {
		if (data == null) {
			return;
		}
		if (mAdapter != null) {
			mAdapter.clear();
		}
		mAdapter = new BannerAdapter();
		mViewPager.setAdapter(mAdapter);
		mAdapter.setData(data);
		if (isShowIndicator) {
			generatePoint();
		}
	}

	private void generatePoint() {
		vRadioGroup.removeAllViews();
		int count = mAdapter.getRealCount();
		if (count <= 1) {
			return;
		}
		int w = getContext().getResources().getDisplayMetrics().widthPixels / 40;
		for (int i = 0; i < count; i++) {
			RadioButton radioButton = generateRadioButton(i, w);
			vRadioGroup.addView(radioButton);
		}
		((MarginLayoutParams) vRadioGroup.getLayoutParams()).bottomMargin = w;
	}

	private RadioButton generateRadioButton(int i, int w) {
		RadioButton rb = new RadioButton(getContext());
		rb.setBackgroundResource(mIndicatorId);
		rb.setButtonDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
		rb.setId(i);
		rb.setPadding(0, 0, 0, 0);
		RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(w, w);
		params.leftMargin = w / 2;
		params.rightMargin = w / 2;
		rb.setLayoutParams(params);
		if (i == 0) {
			rb.setChecked(true);
		}
		return rb;
	}

	public void setIndicatorParent(RadioGroup parent) {
		if (isShowIndicator) {
			vRadioGroup.removeAllViews();
			removeView(vRadioGroup);
			vRadioGroup = parent;
			vRadioGroup.setGravity(Gravity.CENTER);
			vRadioGroup.setOrientation(LinearLayout.HORIZONTAL);

		}
	}

	public void startScroll() {
		if (getCount() > 1) {
			mViewPager.startScroll();
		}
	}

	public int getCount() {
		return mAdapter.getRealCount();
	}

	public void stopScroll() {
		mViewPager.stopScroll();
	}

	public boolean isEmpty() {
		return mAdapter.getCount() == 0;
	}

	public void setOnPageSelectedListener(OnPageSelectedListener l) {
		this.onPageSelectedListener = l;
	}

	public void setHost(Object host) {
		this.host = host;
	}

	public interface IImage {
		String getImgURL();

		void onClick();
	}

	public interface OnPageSelectedListener {
		void onPageSelected(int position, IImage image);
	}

	private class BannerAdapter extends PagerAdapter {
		private List<IImage> mBannerList = new ArrayList<>();
		private List<XImageView> mViewList = new ArrayList<>();
		private List<FrameLayout> mViewGroupList = new ArrayList<>();
		private DisplayImageOptions build = new DisplayImageOptions.Builder().bitmapConfig(Bitmap.Config.RGB_565)
				.cacheOnDisk(true).cacheInMemory(true).build();
		private DisplayImageOptions tipBuild = new DisplayImageOptions.Builder().bitmapConfig(Bitmap.Config.RGB_565)
				.cacheOnDisk(true).cacheInMemory(true).build();

		public void setData(List<? extends IImage> t) {
			clear();
			mBannerList.addAll(t);
			for (int i = 0, length = mBannerList.size(); i < length; i++) {
				mViewList.add(null);
				mViewGroupList.add(null);
			}
			notifyDataSetChanged();
			if (getRealCount() > 1) {
				mViewPager.setCurrentItem(getRealCount() * 2048);
				startScroll();
			} else {
				stopScroll();
			}
		}

		void clear() {
			mBannerList.clear();
			mViewList.clear();
			mViewGroupList.clear();
			stopScroll();
		}

		int getRealCount() {
			return mBannerList.size();
		}

		@Override
		public int getCount() {
			int realCount = getRealCount();
			return realCount < 2 ? realCount : Integer.MAX_VALUE;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			position = position % getRealCount();
			final IImage bean = mBannerList.get(position);
			View view;
			XImageView image;
			if (mPageMargin > 0) {
				FrameLayout parent = mViewGroupList.get(position);
				image = mViewList.get(position);
				if (parent == null || parent.getParent() instanceof ViewGroup || image == null) {
					parent = new FrameLayout(getContext());
					parent.setPadding(mPageMargin, (int) (mPageMargin * getZoomSize()), mPageMargin, (int) (mPageMargin * getZoomSize()));
					image = new XImageView(getContext());
					parent.addView(image, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
					mViewList.set(position, image);
					mViewGroupList.set(position, parent);
				}
				if (isCorner) {
					image.setRoundCorner(UIUtils.dp2px(getContext(), 15));
				}
				image.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						bean.onClick();
						if (host instanceof Dialog) {
							((Dialog) host).dismiss();
						}
					}

				});
				container.addView(parent, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				view = parent;
			} else {
				image = mViewList.get(position);
				if (image == null || image.getParent() instanceof ViewGroup) {
					image = new XImageView(getContext());
					if (isCorner) {
						image.setRoundCorner(UIUtils.dp2px(getContext(), 15));
					}
					image.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							bean.onClick();
							if (host instanceof Dialog) {
								((Dialog) host).dismiss();
							}
						}

					});
					mViewList.set(position, image);
				}
				container.addView(image, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				view = image;
			}
			String imgUrl = bean.getImgURL();
			int resId = 0;
			try {
				resId = Integer.parseInt(imgUrl);
			} catch (Exception ignored) {
			}
			image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			if (resId != 0) {
				image.setImageResource(resId);
			} else {
				INSTANCE.displayImage(imgUrl, image, isCorner ? tipBuild : build, new SimpleImageLoadingListener() {
					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
						((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
					}
				});
			}
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

	}

}