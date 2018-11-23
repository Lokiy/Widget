package com.lokiy.widget.sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.lokiy.control.WidgetConfig;
import com.lokiy.view.XImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		XImageView img = (XImageView) findViewById(R.id.img);
		final ImageLoader instance = ImageLoader.getInstance();

		new WidgetConfig.Builder().imageLoader(new com.lokiy.control.ImageLoader() {
			@Override
			public void loadImage(XImageView imageView, String uri) {
				Log.e("XXX", "--------------------" + uri);
				instance.displayImage(uri, imageView);
			}
		}).build().apply();

		instance.init(ImageLoaderConfiguration.createDefault(this));
//		img.setImageURI(Uri.parse("res://" + getPackageName() + "/" + R.mipmap.ic_launcher));
		img.setImageURI(Uri.parse("http://c.hiphotos.baidu.com/image/pic/item/77c6a7efce1b9d16efbcc03afedeb48f8c546475.jpg"));
	}
}
