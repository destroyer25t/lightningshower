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
import java.util.Date;

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

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
        int heightBackButton = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 110, r.getDisplayMetrics());


        for (File aFile : files)
            if (aFile.isDirectory() && aFile.listFiles().length != 0) {
                Button btn = new Button(this);  //кнопка верхнего слоя
                FrameLayout backFrame = new FrameLayout(this);  //белая подложка
                TextView textView = new TextView(this);

                //получаем информацию о файле
                File file[] = aFile.listFiles();
                File firstFile = file[0];   //первая картинка в папке, для создания эскиза

                Date lastModDate = new Date(aFile.lastModified());  //дата съемки
                String dateAndTime = lastModDate.toString();    //в строковом виде
                String countFiles = Integer.toString(aFile.listFiles().length); //количество фоток в папке(и файлов вообще)
                Spanned text = (Html.fromHtml(aFile.getName()));


                //настройка кнопки верхнего слоя
                btn.setLayoutParams(lButtonParams);
                btn.setHeight(height);
                btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);


                backFrame.setElevation(2);
                backFrame.setBackgroundColor(Color.WHITE);
                backFrame.setPadding(1, 1, 1, 1);

                Bitmap bm = decodeSampledBitmapFromUri(firstFile.getAbsolutePath(), 800, height);
                //Bitmap myBitmap = BitmapFactory.decodeFile(firstFile.getAbsolutePath());
                //Bitmap croppedBitmap = Bitmap.createBitmap(myBitmap, 10, 10, myBitmap.getWidth()-10, height);
                Drawable d = new BitmapDrawable(getResources(), bm);
                btn.setBackground(d);


                final String pathToAfile = aFile.getAbsolutePath();
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), imagesGridView.class);
                        intent.putExtra("folderPath", pathToAfile);    //отправляем в активити адрес
                        startActivity(intent);
                        // Toast.makeText(getBaseContext(), "Click", Toast.LENGTH_SHORT).show();
                    }
                });
                btn.setText(text);

                //настройка надписи под кнопкой
                textView.setText(countFiles + " молний\n" + dateAndTime);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                textView.setLayoutParams(lButtonParams);
                //суем кнопки и надпись во фрейм и фрейм во фрейм
                backFrame.addView(btn);
                backFrame.addView(textView);
                hLayout.addView(backFrame);
                //  hLayout.addView(btn);
                // hLayout.addView(textView);
            }

    }

}

