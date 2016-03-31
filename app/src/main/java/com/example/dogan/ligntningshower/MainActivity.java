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

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private int typeOfHandling = 0;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private static final String TAG = "Lightning Shower Log";
    private Uri imageUri = null;
    SharedPreferences prefs;

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
            case Pick_image:
                if (resultCode == RESULT_OK) {
                    imageUri = imageReturnedIntent.getData();
                    String videopath = getPath(this, imageUri);
                    try {
                        Decomposing(videopath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


        }
    }

    public void Decomposing(String videopath) throws IOException {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String typeOfDecomposing = prefs.getString("pref_decompose_mode","OPENCVdecomposing");
        if (Objects.equals(typeOfDecomposing, "OPENCVdecomposing")) {
            javaCV_decomposing(videopath);
        } else {
           MediaMetadataRetriever_decomposing(videopath);
        }

    }

    /**
     * Функция для декомпозиции методом MediaMetadataRetriever.
     *
     * @param videopath Путь к видео.
     * @author Oleg Zepp
     */
    public void MediaMetadataRetriever_decomposing(String videopath) throws IOException {
        MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
        OpenCVHandler openCVHandler = new OpenCVHandler();
        Bitmap frame;
        String videofileName = getFileName(videopath);
        //устанавливаем источник для mediadata
        mediaMetadata.setDataSource(videopath);


        //получаем длину видео в миллисекундах
        String stringDuration = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);    //длина видео в микросекундах
        int durationMs = Integer.parseInt(stringDuration);    //миллисекундах
        int durationS = durationMs / 1000;    //секундах

       // Toast.makeText(this, "Длина видео:" + durationS, Toast.LENGTH_LONG).show();


        for (int currentFrame = 33333; currentFrame < durationMs * 1000; currentFrame += 33333) {
            long startTime = System.currentTimeMillis();    //засекаем время получения кадра
            frame = mediaMetadata.getFrameAtTime(currentFrame, MediaMetadataRetriever.OPTION_CLOSEST);
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Время выдергивания из видоса: " + ((endTime - startTime) / 1000f));
            openCVHandler.preparingBeforeFindContours(frame, currentFrame, videofileName);
        }

        if (typeOfHandling == 2) {
            boolean isSaveSourceVideo;
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            isSaveSourceVideo = prefs.getBoolean("isSaveSourceVideo", true);
            if (!isSaveSourceVideo) {
                deleteVideoAfterProcessing();
            }
        }

    }


    /**
     * Функция для декомпозиции методом FFmpeg.
     *
     * @param videopath Путь к видео.
     * @author Oleg Zepp
     */
    public void javaCV_decomposing(String videopath) {
        //File videoFile = new File(videopath); to delete
        Bitmap bitmapVideoFrame;
        Frame videoframe = null;
        int framesCounter = 0;
        String videofileName = getFileName(videopath);
        OpenCVHandler openCVHandler = new OpenCVHandler();

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videopath);
        AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();

        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        while (true) {
            long startTime = System.currentTimeMillis();    //засекаем время получения кадра
            try {
                videoframe = grabber.grab();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            bitmapVideoFrame = converterToBitmap.convert(videoframe);
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Время выдергивания из видоса OPENCV: " + ((endTime - startTime) / 1000f));
            framesCounter++;
            openCVHandler.preparingBeforeFindContours(bitmapVideoFrame,framesCounter,videofileName);
        }


    }

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

    void deleteVideoAfterProcessing() {

        String videoPathToDelete = getPath(this, imageUri);
        Log.d(TAG, "Надо бы удалить " + videoPathToDelete);
        File videoFile = new File(videoPathToDelete);
        videoFile.delete();

    }

    public static String getFileName(String fullpath) {
        String[] split = fullpath.split("/");
        String nameOfFile = split[split.length - 1];
        return nameOfFile;
    }


}
