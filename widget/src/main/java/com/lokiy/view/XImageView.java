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
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.lokiy.control.WidgetConfig;
import com.lokiy.widget.R;

import java.util.Locale;

/**
 * 自动加载图片控件
 *
 * @author Luki
 */
@SuppressWarnings({"deprecation", "unused"})
public class XImageView extends android.support.v7.widget.AppCompatImageView {

	private static final String TAG = XImageView.class.getSimpleName();
	private static final int MAX_RETRY_TIMES = 3;
	private static final Type DEFAULT_TYPE = Type.NONE;
	private static final int DEFAULT_BORDER_COLOR = Color.TRANSPARENT;
	private static final int DEFAULT_BORDER_WIDTH = 0;
	private static final int DEFAULT_RECT_ROUND_RADIUS = 0;
	private final int TASK_IMAGE = 0x01;
	private final int TASK_BACKGROUND = 0x02;
	private Type mType;
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
	private float mZoomSize = 0;
	private boolean isLoadingImage, isEmptyShow = false, isParentListView, isParentRecyclerView;
	private int mDefaultImage;
	private static String urlPrefix;

	public enum Type {
		NONE(0), CIRCLE(1),//圆形
		RECT(2);//圆角矩形
		private final int type;

		Type(int type) {
			this.type = type;
		}

		static Type getType(int type) {
			for (Type t : Type.values()) {
				if (t.type == type) {
					return t;
				}
			}
			return NONE;
		}
	}

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

		mType = Type.getType(a.getInt(R.styleable.XImageView_type, DEFAULT_TYPE.ordinal()));
		mRoundColor = a.getColor(R.styleable.XImageView_roundColor, Color.TRANSPARENT);
		mBorderWidth = a.getDimensionPixelSize(R.styleable.XImageView_borderWidth, getDefaultBorderSize());
		mRectRoundRadius = a.getDimensionPixelOffset(R.styleable.XImageView_cornerRadii, 0);
		a.recycle();

		if (mType != Type.NONE) {
			if (getBackground() != null) {
				setBackgroundDrawable(getBackground());
			}
			if (getDrawable() != null) {
				setImageDrawable(getDrawable());
			}
		}
	}

	private int getDefaultBorderSize() {
		float scale = getResources().getDisplayMetrics().density;
		return (int) (XImageView.DEFAULT_BORDER_WIDTH * scale + 0.5f);
	}

	public void setRound(int borderWidth, int roundColor) {
		mType = Type.CIRCLE;
		this.mBorderWidth = borderWidth;
		this.mRoundColor = roundColor;
	}

	public void setRoundCorner(float radius) {
		mType = Type.RECT;
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

	private void setDefaultDrawable() {
		if (!isEmptyShow) {
			int res = mDefaultImage;
			Drawable mTempDrawable;
			if (res == 0) {
				mTempDrawable = new ColorDrawable(0);
			} else {
				if (mType != Type.NONE) {
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
	public void setImageResource(int resId) {
		super.setImageResource(resId);
	}


	public void setImageURI(String uri){
		setImageURI(Uri.parse(uri));
	}

	@Override
	public void setImageURI(Uri uri) {
		if (uri != null) {
			String scheme = uri.getScheme();
			if (!TextUtils.isEmpty(scheme)) {
				switch (scheme.toLowerCase(Locale.getDefault())) {
					case "http":
					case "https":
						WidgetConfig.getConfig().getImageLoader().loadImage(this, uri.toString());
						break;
					case "res":
						setImageResource(Integer.parseInt(uri.getLastPathSegment()));
						break;
					case "file":
						if ("android_asset".equalsIgnoreCase(uri.getHost())) {
							try {
								String path = uri.getPath();
								if (!TextUtils.isEmpty(path)) {
									setImageBitmap(BitmapFactory.decodeStream(getContext().getAssets().open(path)));
								} else {
									throw new Exception("uri:" + uri.toString() + " path is null");
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {//FIXME
							super.setImageURI(uri);
						}
						break;
					default:
						super.setImageURI(uri);
						break;
				}
			} else {
				super.setImageURI(uri);
			}
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

		if (rawBitmap != null && mType != Type.NONE) {
			int viewWidth = getWidth();
			int viewHeight = getHeight();
			int viewMinSize = Math.min(viewWidth, viewHeight);
			float dstWidth = mType == Type.CIRCLE ? viewMinSize : viewWidth;
			float dstHeight = mType == Type.CIRCLE ? viewMinSize : viewHeight;
			float halfBorderWidth = mBorderWidth / 2.0f;
			float doubleBorderWidth = mBorderWidth * 2;

			if (mShader == null || !rawBitmap.equals(mRawBitmap)) {
				mRawBitmap = rawBitmap;
				mShader = new BitmapShader(mRawBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
			}

			mMatrix.setScale((dstWidth - doubleBorderWidth) / rawBitmap.getWidth(), (dstHeight - doubleBorderWidth) / rawBitmap.getHeight());
			mShader.setLocalMatrix(mMatrix);

			mPaintBitmap.setShader(mShader);
			mPaintBorder.setStyle(Paint.Style.STROKE);
			mPaintBorder.setStrokeWidth(mBorderWidth);
			mPaintBorder.setColor(mBorderWidth > 0 ? mRoundColor : Color.TRANSPARENT);

			if (mType == Type.CIRCLE) {
				float radius = viewMinSize / 2.0f;
				canvas.drawCircle(radius, radius, radius - halfBorderWidth, mPaintBorder);
				canvas.translate(mBorderWidth, mBorderWidth);
				canvas.drawCircle(radius - mBorderWidth, radius - mBorderWidth, radius - mBorderWidth, mPaintBitmap);
			} else if (mType == Type.RECT) {
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
}
