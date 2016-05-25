package com.example.dogan.ligntningshower;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static com.example.dogan.ligntningshower.SupportFunctions.decodeSampledBitmapFromUri;

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
                //btn.setMaxHeight(height);
                btn.setHeight(height);
                //btn.set
                btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                File file[] = aFile.listFiles();
                File firstFile = file[0];
                Bitmap bm = decodeSampledBitmapFromUri(firstFile.getAbsolutePath(), 800, height);
                //Bitmap myBitmap = BitmapFactory.decodeFile(firstFile.getAbsolutePath());
                //Bitmap croppedBitmap = Bitmap.createBitmap(myBitmap, 10, 10, myBitmap.getWidth()-10, height);
                Drawable d = new BitmapDrawable(getResources(), bm);
                btn.setBackground(d);
                Date lastModDate = new Date(aFile.lastModified());
                String dateAndTime = lastModDate.toString();
                String countFiles = Integer.toString(aFile.listFiles().length);
                Spanned text = (Html.fromHtml("<b>" + aFile.getName() + "</b>" + "<br />" +
                        "<small>" + countFiles + "</small>" + "<br />" +
                        "<small>" + dateAndTime + "</small>"));

                final String pathToAfile = aFile.getAbsolutePath();
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), imagesGridView.class);
                        intent.putExtra("folderPath", pathToAfile);    //отправляем в активити адрес
                        startActivity(intent);
                        Toast.makeText(getBaseContext(), "Click", Toast.LENGTH_SHORT).show();
                    }
                });
                btn.setText(text);
                hLayout.addView(btn);
            }

    }

}

