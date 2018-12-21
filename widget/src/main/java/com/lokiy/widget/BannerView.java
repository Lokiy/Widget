/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * 　　　http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lokiy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
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

import com.lokiy.control.WidgetConfig;
import com.lokiy.view.XImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link #setData(List)} set banner data.
 * {@link #setHost(Object)}
 * {@link #setIndicatorParent(RadioGroup)}
 * {@link #setOnPageSelectedListener(OnPageSelectedListener)}
 * {@link #setZoomSize(float)}
 * {@link #getCount()}
 *
 * @author Luki
 */
public class BannerView extends ZoomFrameLayout {

    private final int mPageMargin;
    private final int mRectRoundRadius;
    private XViewPager mViewPager;
    private BannerAdapter mAdapter;
    private RadioGroup vRadioGroup;
    private boolean isShowIndicator;
    private int mIndicatorId;
    private OnPageSelectedListener onPageSelectedListener;
    private OnPageClickListener onPageClickListener;
    private Object host;
    private boolean isCorner;
    private boolean isStopScroll;

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
        mRectRoundRadius = a.getDimensionPixelOffset(R.styleable.BannerView_cornerRadii, 0);
        mPageMargin = a.getDimensionPixelSize(R.styleable.BannerView_pageMargin, 0);
        int mIndicatorGravity = a.getInt(R.styleable.BannerView_indicator_gravity, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        a.recycle();
        mViewPager = new XViewPager(getContext(), null);
        mViewPager.setPageMargin(mPageMargin);
        setClipChildren(false);
        mAdapter = new BannerAdapter();
        //noinspection deprecation
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int position) {
                position = position % mAdapter.getRealCount();
                if (isShowIndicator) {
                    vRadioGroup.check(position);
                }
                if (onPageSelectedListener != null) {
                    onPageSelectedListener.onPageSelected(position, mAdapter.mBannerList.get(position));
                }
            }

            @Override
            public void onPageScrollStateChanged(int position) {
            }
        });
        mViewPager.setAdapter(mAdapter);
        addView(mViewPager, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        if (isShowIndicator) {
            vRadioGroup = new RadioGroup(getContext());
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = mIndicatorGravity;
            vRadioGroup.setGravity(Gravity.CENTER);
            vRadioGroup.setOrientation(LinearLayout.HORIZONTAL);
            addView(vRadioGroup, params);
        }
    }

    public XViewPager getViewPager() {
        return mViewPager;
    }

    public void setData(List<? extends IBanner> data) {
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

    public RadioGroup getIndicatorParent() {
        return vRadioGroup;
    }

    /**
     * call before {@link #setData(List)}
     *
     * @param parent RadioGroup
     */
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
        isStopScroll = false;
        startScroll(0);
    }

    public void startScroll(int scrollTimeMillis) {
        if (getCount() > 1) {
            if (scrollTimeMillis > 0) {
                mViewPager.setScrollTimeDelay(scrollTimeMillis);
            }
            mViewPager.startScroll();
        }
    }

    public int getCount() {
        return mAdapter.getRealCount();
    }

    public void stopScroll() {
        isStopScroll = true;
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

    public void setOnPageClickListener(OnPageClickListener onPageClickListener) {
        this.onPageClickListener = onPageClickListener;
    }

    /**
     * 暂停
     */
    public void pause() {
        mViewPager.pause();
    }

    /**
     * 继续
     */
    public void resume() {
        mViewPager.resume();
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param position Item index to select
     */
    public void setCurrentItem(int position) {
        int realCount = getCount();
        if (position >= realCount) {
            throw new IndexOutOfBoundsException(String.format(Locale.getDefault(), "length=%d; index=%d", realCount, position));
        }
        int currentItem = mViewPager.getCurrentItem();
        int absPosition = currentItem % realCount;
        int targetItem = currentItem + position - absPosition;
        mViewPager.setCurrentItem(targetItem);
    }

    public interface IBanner {
        String getImgURL();

        /**
         * instead of {@link OnPageClickListener#onPageClick(int, IBanner, View, BannerView, Object)}
         *
         * @deprecated
         */
        @Deprecated
        void onClick();
    }

    public interface OnPageSelectedListener {
        void onPageSelected(int position, IBanner image);
    }

    /**
     * on page click listener
     */
    public interface OnPageClickListener {
        /**
         * on page clicked
         *
         * @param position position
         * @param iBanner bean
         * @param view clicked view
         * @param bannerView parent view .. bannerView
         * @param host see more {@link #setHost(Object)}
         */
        void onPageClick(int position, IBanner iBanner, View view, BannerView bannerView, Object host);
    }

    private class BannerAdapter extends PagerAdapter {
        private List<IBanner> mBannerList = new ArrayList<>();
        private List<XImageView> mViewList = new ArrayList<>();
        private List<FrameLayout> mViewGroupList = new ArrayList<>();

        public void setData(List<? extends IBanner> t) {
            clear();
            mBannerList.addAll(t);
            for (int i = 0, length = mBannerList.size(); i < length; i++) {
                mViewList.add(null);
                mViewGroupList.add(null);
            }
            notifyDataSetChanged();
            if (getRealCount() > 1) {
                mViewPager.setCurrentItem(getRealCount() * 2048);
                if (!isStopScroll) {
                    startScroll();
                }
            } else {
                mViewPager.stopScroll();
            }
        }

        void clear() {
            mBannerList.clear();
            mViewList.clear();
            mViewGroupList.clear();
            mViewPager.stopScroll();
        }

        int getRealCount() {
            return mBannerList.size();
        }

        @Override
        public int getCount() {
            int realCount = getRealCount();
            return realCount < 2 ? realCount : Integer.MAX_VALUE;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            position = position % getRealCount();
            final IBanner bean = mBannerList.get(position);
            View view;
            XImageView image;
            final int finalPosition = position;
            OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bean.onClick();
                    if (onPageClickListener != null) {
                        onPageClickListener.onPageClick(finalPosition, bean, v, BannerView.this, host);
                    }
                }

            };
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
                view = parent;
            } else {
                image = mViewList.get(position);
                if (image == null || image.getParent() instanceof ViewGroup) {
                    image = new XImageView(getContext());
                    mViewList.set(position, image);
                }
                view = image;
            }
            if (isCorner) {
                image.setRoundCorner(mRectRoundRadius);
            }
            image.setOnClickListener(onClickListener);
            String imgUrl = bean.getImgURL();
            int resId = 0;
            try {
                resId = Integer.parseInt(imgUrl);
            } catch (Exception ignored) {
            }
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            if (resId != 0) {
                image.setImageResource(resId);
            } else {
                WidgetConfig.getConfig().getImageLoader().loadImage(image, imgUrl);
            }
            container.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View arg0, @NonNull Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

    }

}
