package com.example.dogan.ligntningshower;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;

public class navdrawer extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private int typeOfHandling = 0;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navdrawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //Выбор на радиобаттоне - с камеры или из галереи
    public void onClickbutStart(View view) {
        RadioButton mRadButFromCamera = (RadioButton) findViewById(R.id.radButFromCamera);
        RadioButton mRadButFromPhone = (RadioButton) findViewById(R.id.radButFromPhone);
        //RadioButton mRadButFROMopencv = (RadioButton) findViewById(R.id.radButLiveCamera);
        Button buttonStart = (Button) findViewById(R.id.butStart);


        if (mRadButFromCamera.isChecked()) {
            typeOfHandling = 2;
            dispatchTakeVideoIntent();
        } else if (mRadButFromPhone.isChecked()) {
            typeOfHandling = 1;
            Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            photoPickerIntent.setType("video/*");
            startActivityForResult(photoPickerIntent, Pick_image);
        }/* else if (mRadButFROMopencv.isChecked()) {
            typeOfHandling = 3;
            Intent liveCameraIntent = new Intent(MainActivity.this, OpenCVCameraActivity.class);
            startActivity(liveCameraIntent);
        }*/ else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Выберите источник видео!",
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }


    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case Pick_image:                                //если это Pick_image
                if (resultCode == RESULT_OK) {                  //и с ним все нормально
                    imageUri = imageReturnedIntent.getData();               //получаем адрес медифайла
                    Intent processingVideoIntent = new Intent(navdrawer.this, ProcessingActivity.class);
                    processingVideoIntent.putExtra("imageUri", imageUri.toString());    //отправляем в активити адрес
                    processingVideoIntent.putExtra("tOfHand", typeOfHandling);
                    startActivity(processingVideoIntent);
                }


        }

    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main) {

        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent();
            intent.setClass(this, enchancedgallery.class);
            startActivity(intent);

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_about) {
            Intent intent = new Intent();
            intent.setClass(this, about.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
