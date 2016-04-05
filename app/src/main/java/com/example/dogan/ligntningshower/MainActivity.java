package com.example.dogan.ligntningshower;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.example.dogan.ligntningshower.SupportFunctions.*;


public class MainActivity extends AppCompatActivity {

    private int typeOfHandling = 0;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private Uri imageUri = null;


    //Стандартная инициализация активити
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView mImageView = (ImageView) findViewById(R.id.testImageView);
    }

    //Активация и показ меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivity(intent);
        return true;
    }

    //Выбор на радиобаттоне - с камеры или из галереи
    public void onClickbutStart(View view) {
        RadioButton mRadButFromCamera = (RadioButton) findViewById(R.id.radButFromCamera);
        RadioButton mRadButFromPhone = (RadioButton) findViewById(R.id.radButFromPhone);
        RadioButton mRadButFROMopencv = (RadioButton) findViewById(R.id.radButLiveCamera);
        Button buttonStart = (Button) findViewById(R.id.butStart);

        if (mRadButFromCamera.isChecked()) {
            typeOfHandling = 2;
            dispatchTakeVideoIntent();
        } else if (mRadButFromPhone.isChecked()) {
            typeOfHandling = 1;
            Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            photoPickerIntent.setType("video/*");
            startActivityForResult(photoPickerIntent, Pick_image);
        } else if (mRadButFROMopencv.isChecked()) {
            typeOfHandling = 3;
            Intent liveCameraIntent = new Intent(MainActivity.this, OpenCVCameraActivity.class);
            startActivity(liveCameraIntent);
        }


    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    //Принимаем результаты из Активити
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case Pick_image:                                //если это Pick_image
                if (resultCode == RESULT_OK) {                  //и с ним все нормально
                    imageUri = imageReturnedIntent.getData();               //получаем адрес медифайла
                    Intent processingVideoIntent = new Intent(MainActivity.this, ProcessingActivity.class);
                    processingVideoIntent.putExtra("imageUri", imageUri.toString());    //отправляем в активити адрес
                    startActivity(processingVideoIntent);
                }


        }

        if (typeOfHandling == 2) {
            boolean isSaveSourceVideo;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            isSaveSourceVideo = prefs.getBoolean("isSaveSourceVideo", true);
            if (!isSaveSourceVideo) {
                deleteVideoAfterProcessing(this, imageUri);
            }
        }

    }





}
