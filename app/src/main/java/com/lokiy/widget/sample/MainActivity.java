package com.lokiy.widget.sample;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lokiy.view.XImageView;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		XImageView img = (XImageView) findViewById(R.id.img);
		img.setImageURI(Uri.parse("res://" + getPackageName() + "/" + R.mipmap.ic_launcher));
	}
}
