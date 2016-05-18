package com.example.dogan.ligntningshower;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class enchancedgallery extends AppCompatActivity {
    private Vector<ImageView> mySDCardImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enchancedgallery);
        mySDCardImages = new Vector<ImageView>();

        final ImageView diplayImage = (ImageView) findViewById(R.id.displayImage);
        final LinearLayout myGallery = (LinearLayout) findViewById(R.id.mygallery);

        try {
            String galleryDirectoryName = "gallery";
            String[] listImages = getAssets().list(galleryDirectoryName);
            for (String imageName : listImages) {
                InputStream is = getAssets().open(galleryDirectoryName + "/" + imageName);
                final Bitmap bitmap = BitmapFactory.decodeStream(is);

                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(70, 70));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        diplayImage.setImageBitmap(bitmap);
                    }
                });

                myGallery.addView(imageView);
            }
        } catch (IOException e) {
            Log.e("GalleryWithHorizontalScrollView", e.getMessage(), e);
        }

    }

}

