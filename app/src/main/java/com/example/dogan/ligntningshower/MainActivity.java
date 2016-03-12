package com.example.dogan.ligntningshower;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private final int Pick_image = 1;
    private static final String TAG = "Lightning Shower Log";

    private SharedPreferences mSettings;

    //Стандартная инициализация активити
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView mImageView = (ImageView) findViewById(R.id.testImageView);
    }

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

        if (mRadButFromCamera.isChecked()) {
            Intent intent = new Intent(MainActivity.this, CameraAppActivity.class);
            startActivity(intent);
        } else if (mRadButFromPhone.isChecked()) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            photoPickerIntent.setType("video/*");
            startActivityForResult(photoPickerIntent, Pick_image);
        }
    }

    //Принимаем результаты из Активити
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case Pick_image:
                if (resultCode == RESULT_OK) {
                    // try{
                    //Получаем URI, преобразуем в битмап, отображаем в ImageView
                    final Uri imageUri = imageReturnedIntent.getData();
                    String videopath = getPath(this, imageUri);
                    //Toast.makeText(this, "Путь к видео:\n" +
                         //   videopath, Toast.LENGTH_LONG).show();
                    Decomposing(videopath);
                    /*ExtractMpegFramesTest emft=new ExtractMpegFramesTest();
                    try {
                        emft.testExtractMpegFrames();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }*/
                }
        }
    }

    public void Decomposing(String videopath){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String regular="";
        MediaMetadataRetriever_decomposing(videopath);
        // получаем выбранный пользователем способ раскадровки
       /* if (prefs.contains("pref_decompose_mode")) {
            regular = prefs.getString(getString(R.string.pref_decompose_mode), "");
        }

        if(regular=="OPENCV decomposing") {

        }

        if(regular=="MediaMetadataRetriever") {

        }*/
    }

    public void MediaMetadataRetriever_decomposing(String videopath){
        Double fps = 30.0;
        MediaMetadataRetriever mediaMetadata=new MediaMetadataRetriever();
        mediaMetadata.setDataSource(videopath);
        Bitmap frame=null;
        Long incrementer = (long) (1000000 / fps);

        String stringDuration = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);    //длина видео в микросекундах
        Double durationS = Double.parseDouble(stringDuration)/1000;    //секундах
        Double hours = durationS / 3600;   //часах
        Double minutes = (durationS - hours * 3600) / 60;      //минутах

        Toast.makeText(this, "Длина видео:"+durationS, Toast.LENGTH_LONG).show();
        int FRAME_BYTES=326;
        int FRAMESMAX=36;
       // String mediaFileName="source.mp4";
        //String filePath=Environment.getExternalStorageDirectory().getPath()+File.separator+mediaFileName;
/*
        try{
            mediaMetadata.setDataSource(videopath);


            for(int currentFrame=0;currentFrame<FRAMESMAX; currentFrame++){
                frame=null;
                if(currentFrame<=0){
                    frame = mediaMetadata.getFrameAtTime();
                }else{
                    frame =   mediaMetadata.getFrameAtTime(FRAME_BYTES*currentFrame*1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC );
                    //currentFrame++;
                }

                Log.d(TAG, "Height of frame:"+frame.getHeight());
            }
        }catch(Exception e){
            Log.i(TAG, "  unable to get file descriptor of the frame"+e.toString());
        }*/
    }


    //Запись видео в этом же каком-то стандартном активити VIDEO_CAPTURE
   /* public void onClickbutStart(View view)
    {
       // RadioButton mRadButFromCamera = (RadioButton)findViewById(R.id.radButFromCamera);
        //if(mRadButFromCamera.isChecked()){
            File mediaFile = new
                    File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "myvideo.mp4");

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            fileUri = Uri.fromFile(mediaFile);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(intent, VIDEO_CAPTURE);
        //}

    }
    //прием видео из активити выше
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Video has been saved to:\n" +
                        data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to record video",
                        Toast.LENGTH_LONG).show();
            }
        }
    }*/

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    public void Test1Action(View view) {

    }
}
