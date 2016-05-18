package com.example.dogan.ligntningshower;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class enchancedgallery extends AppCompatActivity {

    File[] files;
    Resources r;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enchancedgallery);
        r = getResources();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies";
        files = new File(path).listFiles();
        creatingButtons();
    }

    protected void creatingButtons() {
        LinearLayout hLayout = (LinearLayout) findViewById(R.id.galleryHorLayout);
        LinearLayout.LayoutParams lButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 110, r.getDisplayMetrics());


        for (File aFile : files)
            if (aFile.isDirectory() && aFile.listFiles().length != 0) {
                Button btn = new Button(this);
                btn.setLayoutParams(lButtonParams);
                btn.setHeight(height);
                btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                btn.setTextColor(Color.WHITE);
                //btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 21);


                SpannableStringBuilder spanSin = new SpannableStringBuilder();
                SpannableString itemasin = new SpannableString(aFile.getName() + "\n");
                itemasin.setSpan(new AbsoluteSizeSpan(21, true), 0, itemasin.length(), 0);
                spanSin.append(itemasin);

                Date lastModDate = new Date(aFile.lastModified());

                SpannableString itemsin = new SpannableString(lastModDate.toString());
                itemsin.setSpan(new AbsoluteSizeSpan(12, true), 0, itemsin.length(), 0);
                spanSin.append(itemsin);


                //Spannable span = new SpannableString(aFile.getName() +"\n" +  lastModDate.toString());
                //span.setSpan(new AbsoluteSizeSpan(fontSize), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                btn.setText(spanSin, TextView.BufferType.SPANNABLE);
                hLayout.addView(btn);
            }

    }

}

