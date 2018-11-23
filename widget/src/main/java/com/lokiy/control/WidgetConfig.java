package com.lokiy.control;

public class WidgetConfig {
	private static WidgetConfig config;
	private ImageLoader imageLoader;

	private WidgetConfig(Builder builder) {
		imageLoader = builder.imageLoader;
	}

	public void apply() {
		config = this;
	}

	public static boolean isInit() {
		return config != null;
	}

	public static WidgetConfig getConfig() {
		check();
		return config;
	}

	private static void check() {
		if (!isInit()) {
			throw new IllegalArgumentException("请先初始化 WidgetConfig");
		} else if (config.imageLoader == null) {
			throw new IllegalArgumentException("请先设置一种加载图片的方式");
		}
	}

	public ImageLoader getImageLoader() {
		return imageLoader;
	}

	public static Builder createBuilder(){
		return new Builder();
	}

	public static final class Builder {
		private ImageLoader imageLoader;

		public Builder() {
		}

		public Builder imageLoader(ImageLoader val) {
			imageLoader = val;
			return this;
		}

		public WidgetConfig build() {
			return new WidgetConfig(this);
		}
	}
}
