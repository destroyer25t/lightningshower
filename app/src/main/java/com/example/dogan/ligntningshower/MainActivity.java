package com.example.dogan.ligntningshower;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    //private static final int VIDEO_CAPTURE = 101;
   // private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickbutStart(View view) {
        RadioButton mRadButFromCamera = (RadioButton)findViewById(R.id.radButFromCamera);
        if(mRadButFromCamera.isChecked()){
            Intent intent = new Intent(MainActivity.this, CameraAppActivity.class);
            startActivity(intent);
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
