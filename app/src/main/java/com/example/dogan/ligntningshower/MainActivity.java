package com.example.dogan.ligntningshower;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {


    private ImageView mImageView;
    private final int Pick_image=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mImageView=(ImageView)findViewById(R.id.testImageView);
    }

    public void onClickbutStart(View view) {
        RadioButton mRadButFromCamera = (RadioButton)findViewById(R.id.radButFromCamera);
        RadioButton mRadButFromPhone = (RadioButton)findViewById(R.id.radButFromPhone);
        if(mRadButFromCamera.isChecked()){
            Intent intent = new Intent(MainActivity.this, CameraAppActivity.class);
            startActivity(intent);
        }
        else if(mRadButFromPhone.isChecked()){
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, Pick_image);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent){
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode){
            case Pick_image:
                if(resultCode==RESULT_OK){
                    try{
                        //Получаем URI, преобразуем в битмап, отображаем в ImageView
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        mImageView.setImageBitmap(selectedImage);

                    } catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
        }
    }

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
}
