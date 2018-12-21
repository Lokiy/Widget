package com.lokiy.widget.sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.lokiy.control.WidgetConfig;
import com.lokiy.view.XImageView;
import com.lokiy.widget.BannerView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        XImageView img = findViewById(R.id.img);
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
//        img.setImageURI(Uri.parse("http://c.hiphotos.baidu.com/image/pic/item/77c6a7efce1b9d16efbcc03afedeb48f8c546475.jpg"));


        BannerView banner = findViewById(R.id.banner);
        List<BannerView.IBanner> data = new ArrayList<>();
        BannerView.IBanner iBanner = new BannerView.IBanner() {
            @Override
            public String getImgURL() {
                return "http://c.hiphotos.baidu.com/image/pic/item/77c6a7efce1b9d16efbcc03afedeb48f8c546475.jpg";
            }

            @Override
            public void onClick() {

            }
        };
        data.add(iBanner);
        iBanner = new BannerView.IBanner() {
            @Override
            public String getImgURL() {
                return "http://c.hiphotos.baidu.com/image/pic/item/77c6a7efce1b9d16efbcc03afedeb48f8c546475.jpg";
            }

            @Override
            public void onClick() {

            }
        };
        data.add(iBanner);
//        iBanner = new BannerView.IBanner() {
//            @Override
//            public String getImgURL() {
//                return "http://c.hiphotos.baidu.com/image/pic/item/77c6a7efce1b9d16efbcc03afedeb48f8c546475.jpg";
//            }
//
//            @Override
//            public void onClick() {
//
//            }
//        };
//        data.add(iBanner);
        banner.getViewPager().setOffscreenPageLimit(1);
        banner.setData(data);
    }
}
