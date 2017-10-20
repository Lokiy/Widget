/*
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lokiy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.lokiy.utils.NetStatusUtils;
import com.lokiy.utils.XLog;
import com.lokiy.widget.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;

import java.io.File;

/**
 * 自动加载图片控件
 *
 * @author Luki
 */
@SuppressWarnings({
		"deprecation",
		"unused"
})
public class XImageView extends android.support.v7.widget.AppCompatImageView implements ImageLoadingListener, View.OnClickListener, ImageLoadingProgressListener {

	public static final int TYPE_NONE = 0;
	/**
	 * 圆形
	 */
	public static final int TYPE_CIRCLE = 1;
	/**
	 * 圆角矩形
	 */
	public static final int TYPE_ROUNDED_RECT = 2;
	private static final String TAG = XImageView.class.getSimpleName();
	private static final int MAX_RETRY_TIMES = 3;
	private static final ImageLoader mImageLoader = ImageLoader.getInstance();
	private static final int DEFAULT_TYPE = TYPE_NONE;
	private static final int DEFAULT_BORDER_COLOR = Color.TRANSPARENT;
	private static final int DEFAULT_BORDER_WIDTH = 0;
	private static final int DEFAULT_RECT_ROUND_RADIUS = 0;
	private final int TASK_IMAGE = 0x01;
	private final int TASK_BACKGROUND = 0x02;
	protected int mRetryTimes;
	private int mType;
	private Paint mPaintBitmap = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
	private RectF mRectBorder = new RectF();
	private RectF mRectBitmap = new RectF();
	private Bitmap mRawBitmap;
	private BitmapShader mShader;
	private Matrix mMatrix = new Matrix();
	private boolean isAnimation = false;
	private int mTask = 0;
	private String mUrl;
	private boolean isZoom = false;
	private int mRoundColor = Color.TRANSPARENT;
	private int mBorderWidth;
	private float mRectRoundRadius;
	private Bitmap mLoadedImage;
	private OnLoadImageListener mOnLoadImageListener;
	private float mZoomSize = 0;
	private boolean isLoadingImage, isEmptyShow = false, isParentListView, isParentRecyclerView;
	private int mDefaultImage;
	private String urlPrefix;

	public XImageView(Context context) {
		this(context, null);
	}

	public XImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public XImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XImageView);
		isZoom = a.getBoolean(R.styleable.XImageView_autoZoom, false);
		isAnimation = a.getBoolean(R.styleable.XImageView_isAnim, false);
		mZoomSize = a.getFloat(R.styleable.XImageView_zoomSize, 0f);
		mDefaultImage = a.getResourceId(R.styleable.XImageView_defaultImg, 0);

		mType = a.getInt(R.styleable.XImageView_type, DEFAULT_TYPE);
		mRoundColor = a.getColor(R.styleable.XImageView_roundColor, Color.TRANSPARENT);
		mBorderWidth = a.getDimensionPixelSize(R.styleable.XImageView_borderWidth, dip2px(DEFAULT_BORDER_WIDTH));
		mRectRoundRadius = a.getDimensionPixelOffset(R.styleable.XImageView_cornerRadii, 0);
		a.recycle();

		if (mType != TYPE_NONE) {
			if (getBackground() != null) {
				setBackgroundDrawable(getBackground());
			}
			if (getDrawable() != null) {
				setImageDrawable(getDrawable());
			}
		}

		check();
	}

	private int dip2px(int dipVal) {
		float scale = getResources().getDisplayMetrics().density;
		return (int) (dipVal * scale + 0.5f);
	}

	public void setRound(int borderWidth, int roundColor) {
		mType = TYPE_CIRCLE;
		this.mBorderWidth = borderWidth;
		this.mRoundColor = roundColor;
	}

	public void setRoundCorner(float radius) {
		mType = TYPE_ROUNDED_RECT;
		this.mRectRoundRadius = radius;
	}

	public void setZoom(boolean isZoom) {
		this.isZoom = isZoom;
	}

	/**
	 * @return the mZoomSize
	 */
	public float getZoomSize() {
		return mZoomSize;
	}

	public void setZoomSize(float zoomSize) {
		this.isZoom = false;
		this.mZoomSize = zoomSize;
	}

	/**
	 * 设置是否启用动画显示
	 *
	 * @param isAnimation isAnimation
	 */
	public void setAnimation(boolean isAnimation) {
		this.isAnimation = isAnimation;
	}

	/**
	 * 设置图片加载中不显示默认图片
	 *
	 * @param isEmptyShow true:不显示，false:显示
	 */
	public void setEmptyShow(boolean isEmptyShow) {
		this.isEmptyShow = isEmptyShow;
	}

	public void setBackgroundURL(String url) {
		this.setBackgroundURL(url, isZoom);
	}

	/**
	 * @param url    url
	 * @param isZoom isZoom
	 */
	public void setBackgroundURL(String url, boolean isZoom) {
		mTask = TASK_BACKGROUND;
		this.isZoom = isZoom;
		loadImage(url);
	}

	@Override
	public void onLoadingStarted(String imageUri, View view) {
		setDefaultDrawable();
	}

	@Override
	public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
		loadingStatus(false);
		setDefaultDrawable();
		loadImageFailed(this, imageUri);
	}

	@Override
	public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
		loadingStatus(false);
		if (loadedImage != null) {
			onLoadingComplete(new BitmapDrawable(getResources(), mLoadedImage = loadedImage));
			loadImageSuccess(this, imageUri);
		} else {
			setDefaultDrawable();
		}
	}

	@Override
	public void onLoadingCancelled(String imageUri, View view) {
		loadingStatus(false);
		postDelayed(new Runnable() {

			@Override
			public void run() {
				if (mRetryTimes < MAX_RETRY_TIMES) {
					loadImage(mUrl);
					mRetryTimes++;
				} else {
					XLog.w(TAG, "onLoadingCancelled ==========> url:" + mUrl);
				}
			}
		}, 1000);
	}

	private boolean loadingStatus(boolean loading) {
		boolean isLoading = false;
		synchronized (TAG) {
			if (isLoadingImage) {
				isLoading = true;
			}
			isLoadingImage = loading;
		}
		return isLoading;
	}

	private void loadImage(String url) {
		if (isParentListView) {
			reset();
		}

		if (!mImageLoader.isInited()) {
			initImageLoader(getContext());
		}

		if (loadingStatus(true)) {
			return;
		}
		if (TextUtils.isEmpty(url)) {
			onLoadingFailed(url, this, null);
			return;
		}

		if (!url.contains("http"))
			mUrl = urlPrefix + url;
		else
			mUrl = url;
		if (NetStatusUtils.isNetworkConnected()) {
			if (mDefaultImage != 0) {
				DisplayImageOptions options = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheOnDisk(true).cacheInMemory(true).showImageOnLoading(mDefaultImage).showImageForEmptyUri(mDefaultImage).showImageOnFail(mDefaultImage).build();
				mImageLoader.loadImage(mUrl, null, options, this, this);
			} else
				mImageLoader.loadImage(mUrl, null, null, this, this);
		} else {
			File file = mImageLoader.getDiskCache().get(mUrl);
			if (file != null && file.exists()) {
				mImageLoader.loadImage(mUrl, null, null, this, this);
			} else {
				setDefaultDrawable();
				loadImageFailed(this, mUrl);
			}
		}
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public void reset() {
		mRetryTimes = 0;
		isLoadingImage = false;
		mImageLoader.cancelDisplayTask(this);
		setDefaultDrawable();
	}

	public static synchronized void initImageLoader(Context context) {
		if (!ImageLoader.getInstance().isInited()) {
			DisplayImageOptions defaultDisplayImageOptions = new DisplayImageOptions.Builder()
					.bitmapConfig(Config.RGB_565)
					.cacheOnDisk(true)
					.cacheInMemory(true)
					.build();
			Builder builder = new ImageLoaderConfiguration.Builder(context)
					.threadPriority(Thread.NORM_PRIORITY - 2)
					.denyCacheImageMultipleSizesInMemory()
					.tasksProcessingOrder(QueueProcessingType.LIFO)
					.threadPoolSize(5)
					.defaultDisplayImageOptions(defaultDisplayImageOptions)
					.memoryCacheSizePercentage(60);
			ImageLoaderConfiguration config = builder.build();
			ImageLoader.getInstance().init(config);
		}
	}

	private void loadImageFailed(XImageView view, String uri) {
		if (mOnLoadImageListener != null) {
			mOnLoadImageListener.onLoadImageFailed(this, uri);
		}
	}

	private void setDefaultDrawable() {
		if (!isEmptyShow) {
			int res = mDefaultImage;
			Drawable mTempDrawable;
			if (res == 0) {
				mTempDrawable = new ColorDrawable(0);
			} else {
				if (mType != TYPE_NONE) {
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(), res);
					mTempDrawable = new BitmapDrawable(bitmap);
				} else {
					mTempDrawable = getResources().getDrawable(res);
				}
			}
			if (mTask == TASK_IMAGE) {
				super.setImageDrawable(mTempDrawable);
			} else {
				super.setBackgroundDrawable(mTempDrawable);
			}
		}
	}

	public void setLoadingDrawableRes(int resId) {
		mDefaultImage = resId;
	}

	private void onLoadingComplete(Drawable drawable) {
		if (drawable != null) {
			if (isAnimation) {
				Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
				animation.setDuration(1000);
				animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());
				startAnimation(animation);
			}

			if (mTask == TASK_IMAGE) {
				XImageView.super.setImageDrawable(drawable);
			} else {
				XImageView.super.setBackgroundDrawable(drawable);
			}
		} else {
			setDefaultDrawable();
		}
	}

	@Override
	public void onClick(View v) {
		if (mTask == TASK_IMAGE) {
			setImageURL(mUrl);
		} else {
			setBackgroundURL(mUrl);
		}
	}

	@Override
	public void setBackgroundDrawable(Drawable drawable) {
		if (mTask != TASK_BACKGROUND || drawable instanceof DrawableContainer || drawable instanceof GradientDrawable) {
			super.setBackgroundDrawable(drawable);
		} else if (drawable != null) {
			onLoadingComplete(mUrl, this, drawableToBitmap(drawable));
		}
	}

	public void setOnLoadImageListener(OnLoadImageListener l) {
		this.mOnLoadImageListener = l;
	}

	private void loadImageSuccess(XImageView view, String uri) {
		if (mOnLoadImageListener != null) {
			mOnLoadImageListener.onLoadImageSuccess(this, uri);
		}
	}

	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);
	}

	@Override
	public void setImageURI(Uri uri) {
		if (uri != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
			setImageURL(uri.toString());
		} else {
			super.setImageURI(uri);
		}
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		mTask = TASK_IMAGE;
		if (drawable instanceof DrawableContainer) {
			super.setImageDrawable(drawable);
		} else if (drawable != null) {
			onLoadingComplete(mUrl, this, drawableToBitmap(drawable));
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mLoadedImage != null && isZoom && getVisibility() != GONE) {
			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = (int) ((float) mLoadedImage.getHeight() * w / mLoadedImage.getWidth());
			setMeasuredDimension(w, h);
		} else if (mZoomSize != 0 && getVisibility() != GONE) {
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
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Bitmap rawBitmap = getBitmap(getDrawable());

		if (rawBitmap != null && mType != TYPE_NONE) {
			int viewWidth = getWidth();
			int viewHeight = getHeight();
			int viewMinSize = Math.min(viewWidth, viewHeight);
			float dstWidth = mType == TYPE_CIRCLE ? viewMinSize : viewWidth;
			float dstHeight = mType == TYPE_CIRCLE ? viewMinSize : viewHeight;
			float halfBorderWidth = mBorderWidth / 2.0f;
			float doubleBorderWidth = mBorderWidth * 2;

			if (mShader == null || !rawBitmap.equals(mRawBitmap)) {
				mRawBitmap = rawBitmap;
				mShader = new BitmapShader(mRawBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
			}
			if (mShader != null) {
				mMatrix.setScale((dstWidth - doubleBorderWidth) / rawBitmap.getWidth(), (dstHeight - doubleBorderWidth) / rawBitmap.getHeight());
				mShader.setLocalMatrix(mMatrix);
			}

			mPaintBitmap.setShader(mShader);
			mPaintBorder.setStyle(Paint.Style.STROKE);
			mPaintBorder.setStrokeWidth(mBorderWidth);
			mPaintBorder.setColor(mBorderWidth > 0 ? mRoundColor : Color.TRANSPARENT);

			if (mType == TYPE_CIRCLE) {
				float radius = viewMinSize / 2.0f;
				canvas.drawCircle(radius, radius, radius - halfBorderWidth, mPaintBorder);
				canvas.translate(mBorderWidth, mBorderWidth);
				canvas.drawCircle(radius - mBorderWidth, radius - mBorderWidth, radius - mBorderWidth, mPaintBitmap);
			} else if (mType == TYPE_ROUNDED_RECT) {
				mRectBorder.set(halfBorderWidth + getPaddingLeft(), halfBorderWidth + getPaddingTop(), dstWidth - halfBorderWidth - getPaddingRight(), dstHeight - halfBorderWidth - getPaddingBottom());
				mRectBitmap.set(0.0f, 0.0f, dstWidth - doubleBorderWidth, dstHeight - doubleBorderWidth);
				float borderRadius = mRectRoundRadius - halfBorderWidth > 0.0f ? mRectRoundRadius - halfBorderWidth : 0.0f;
				float bitmapRadius = mRectRoundRadius - mBorderWidth > 0.0f ? mRectRoundRadius - mBorderWidth : 0.0f;
				canvas.drawRoundRect(mRectBorder, borderRadius, borderRadius, mPaintBorder);
				canvas.translate(mBorderWidth, mBorderWidth);
				canvas.drawRoundRect(mRectBitmap, bitmapRadius, bitmapRadius, mPaintBitmap);
			}
		} else {
			super.onDraw(canvas);
		}
	}

	private Bitmap getBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		} else if (drawable instanceof ColorDrawable) {
			Rect rect = drawable.getBounds();
			int width = rect.right - rect.left;
			int height = rect.bottom - rect.top;
			int color = ((ColorDrawable) drawable).getColor();
			Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
			return bitmap;
		} else {
			return null;
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		check();
		if (mLoadedImage == null && getDrawable() != null) {
			mLoadedImage = drawableToBitmap(getDrawable());
		}
	}

	private void check() {
		initImageLoader(getContext());
		View v = this;
		for (int i = 0; i < 5; i++) {
			if (v.getParent() instanceof View && (v = (View) v.getParent()) instanceof AbsListView) {
				isParentListView = true;
				break;
			}
		}
		for (int i = 0; i < 5; i++) {
			if (v.getParent() instanceof View && (v = (View) v.getParent()) instanceof RecyclerView) {
				isParentRecyclerView = true;
				break;
			}
		}
	}

	public Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}
		if (drawable == null) {
			return Bitmap.createBitmap(1, 1, Config.ALPHA_8);
		}

		int width = drawable.getIntrinsicWidth();
		width = width > 0 ? width : (getWidth() > 0 ? getWidth() : 1);
		int height = drawable.getIntrinsicHeight();
		height = height > 0 ? height : (getHeight() > 0 ? getHeight() : 1);

		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (!isParentRecyclerView) {
			reset();
		}
	}

	public void setImageURL(String url) {
		this.setImageURL(url, isZoom);
	}

	/**
	 * @param url    url
	 * @param isZoom isZoom
	 */
	public void setImageURL(String url, boolean isZoom) {
		mTask = TASK_IMAGE;
		this.isZoom = isZoom;
		loadImage(url);
	}

	@Override
	public void onProgressUpdate(String imageUri, View view, int current, int total) {
		if (view instanceof XImageView) {
			progressUpdate((XImageView) view, imageUri, current, total);
		}
	}

	private void progressUpdate(XImageView view, String imageUri, int current, int total) {
		if (mOnLoadImageListener != null) {
			mOnLoadImageListener.onProgressUpdate(this, imageUri, current, total);
		}
	}

	public interface OnLoadImageListener {
		void onLoadImageFailed(XImageView view, String uri);

		void onLoadImageSuccess(XImageView view, String uri);

		void onProgressUpdate(XImageView view, String uri, int current, int total);
	}
}
