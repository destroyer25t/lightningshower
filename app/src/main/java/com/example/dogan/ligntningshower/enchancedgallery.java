package com.example.dogan.ligntningshower;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        hLayout.setPadding(1, 2, 1, 3);

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics());

        for (File aFile : files)
            if (aFile.isDirectory() && aFile.listFiles().length != 0) {

                //получаем наш кастомный layout
                final View view = getLayoutInflater().inflate(R.layout.folderview, null);
                Button buttonOnFolderView = (Button) view.findViewById(R.id.buttonFolder);
                TextView titleOnFolderView = (TextView) view.findViewById(R.id.titleName);
                TextView countTVOnFolderView = (TextView) view.findViewById(R.id.count);
                TextView dateTVOnFolderView = (TextView) view.findViewById(R.id.date);

                //получаем информацию о файле
                File file[] = aFile.listFiles();
                File firstFile = file[0];   //первая картинка в папке, для создания эскиза

                Date lastModDate = new Date(aFile.lastModified());  //дата съемки
                String dateAndTime = lastModDate.toString();    //в строковом виде
                String countFiles = Integer.toString(aFile.listFiles().length); //количество фоток в папке(и файлов вообще)
                Spanned text = (Html.fromHtml(aFile.getName()));

                Bitmap bm = decodeSampledBitmapFromUri(firstFile.getAbsolutePath(), 780, height);
                Drawable d = new BitmapDrawable(getResources(), bm);
                buttonOnFolderView.setBackground(d);


                final String pathToAfile = aFile.getAbsolutePath();
                buttonOnFolderView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        
                        return false;
                    }
                });
                buttonOnFolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), imagesGridView.class);
                        intent.putExtra("folderPath", pathToAfile);    //отправляем в активити адрес
                        startActivity(intent);
                        // Toast.makeText(getBaseContext(), "Click", Toast.LENGTH_SHORT).show();
                    }
                });
                //buttonOnFolderView.setText(text);

                //настройка надписи под кнопкой
                titleOnFolderView.setText(text);
                countTVOnFolderView.setText(countFiles + " молний");
                dateTVOnFolderView.setText(dateAndTime);

                hLayout.addView(view);
            }
    }

}
