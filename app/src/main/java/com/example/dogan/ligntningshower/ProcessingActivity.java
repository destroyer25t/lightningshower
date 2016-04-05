//TODO:JavaCV пашет прекрасно, но AsyncTask блокируются где то через 20 секунд обработки

package com.example.dogan.ligntningshower;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Objects;

import static com.example.dogan.ligntningshower.SupportFunctions.getFileName;
import static com.example.dogan.ligntningshower.SupportFunctions.getPath;

public class ProcessingActivity extends AppCompatActivity {

    MediaMetadataRetriever_Decomposing_Task MediaMRetrTask;
    JavaCVDecomposing_Task JavaCVTask;

    int typeOfTask;
    Uri imageUri;
    String videopath;
    SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        //различные инициализации, получение данных из родительского активити
        String tempUri = getIntent().getExtras().getString("imageUri");
        imageUri = Uri.parse(tempUri);  //преобразуем полученный строкой Uri обратно в Uri
        videopath = getPath(this, imageUri);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            Decomposing(videopath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Decomposing(String videopath) throws IOException {

        String typeOfDecomposing = prefs.getString("pref_decompose_mode", "OPENCVdecomposing");

        //получаем длительность видоса сразу же
        MediaMetadataRetriever mediaMetadataForGettingDuration = new MediaMetadataRetriever();
        mediaMetadataForGettingDuration.setDataSource(videopath);
        String stringDuration = mediaMetadataForGettingDuration.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);    //длина видео в микросекундах
        int durationMs = Integer.parseInt(stringDuration);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videopath);

        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        double frameRate = grabber.getFrameRate();

        if (Objects.equals(typeOfDecomposing, "OPENCVdecomposing")) {
            JavaCVTask = new JavaCVDecomposing_Task(durationMs, frameRate);
            JavaCVTask.execute(videopath);
        } else {
            MediaMRetrTask = new MediaMetadataRetriever_Decomposing_Task(durationMs, frameRate);
            MediaMRetrTask.execute(videopath);
        }

    }


    protected class JavaCVDecomposing_Task extends AsyncTask<String, Integer, Void> {
        //получаем доступ к элементам интерфейса
        ProgressBar horizontalprogress = (ProgressBar) findViewById(R.id.progressBarFrames);
        TextView textviewCountSeconds = (TextView) findViewById(R.id.textViewCountSeconds);
        TextView textViewCountLightnings = (TextView) findViewById(R.id.textViewFoundedLightnings);
        TextView textViewCountFrames = (TextView) findViewById(R.id.textViewCountFrames);
        ImageView imageView = (ImageView) findViewById(R.id.imageViewCurrentFrame);
        Button buttonStop = (Button) findViewById(R.id.buttonStop);

        private int frameRate;
        private int durationMs; //длительность видео
        private int durationS;
        private int frames;

        int counterFrames = 0;        //счетчик кадров
        int counterSeconds = 0;       //счетчик обработанных секунд
        int counterLightnings = 0;     //счетчик молний

        //переопределяем конструктор Task для получения дополнительных параметров
        public JavaCVDecomposing_Task(int duration, double frameRateTemp) {
            super();
            frameRate = (int) frameRateTemp;  //FPS видео
            durationMs = duration;        //продолжительность видео в миллисекундах
            durationS = durationMs / 1000;
            frames = frameRate * durationS;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            typeOfTask = 0;
            horizontalprogress.setMax(durationMs / frameRate);
            horizontalprogress.setProgress(0);

            Toast.makeText(getApplicationContext(), "Обработка видео запущена", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            horizontalprogress.setProgress(progress[0]);
            textviewCountSeconds.setText("Секунд видео обработано:" + String.valueOf(progress[1]) + " из " + String.valueOf(durationS));
            textViewCountLightnings.setText("Молний обнаружено:" + String.valueOf(progress[2]));
            textViewCountFrames.setText("Кадров обработано:" + String.valueOf(progress[0]) + " из " + String.valueOf(frames));
        }

        @Override
        protected Void doInBackground(String... params) {
            Bitmap bitmapVideoFrame;
            Frame videoframe = null;
            int currentFrame = 0;
            String videofileName = getFileName(params[0]);
            OpenCVHandler openCVHandler = new OpenCVHandler();

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(params[0]);
            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();

            try {
                grabber.start();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }

            while (true) {
                if (isCancelled()) return null;
                long startTime = System.currentTimeMillis();    //засекаем время получения кадра

                try {
                    videoframe = grabber.grab();
                    currentFrame = grabber.getFrameNumber();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }


                Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame);
                counterFrames++;
                if (counterFrames == frameRate) {   //каждые FPS кадров сбрасываем счетчик кадров и добавляем секунду
                    counterFrames = 0;
                    counterSeconds++;
                    Log.d("Lightning Shower Debug:", "Секунда видео: " + counterSeconds);
                }


                bitmapVideoFrame = converterToBitmap.convert(videoframe);
                final Bitmap finalBitmapVideoFrame = bitmapVideoFrame;
                runOnUiThread(new Runnable() {
                    public void run() {
                        imageView.setImageBitmap(finalBitmapVideoFrame);

                    }
                });
                long endTime = System.currentTimeMillis();
                Log.d("Lightning Shower Debug:", "Время выдергивания из видоса OPENCV: " + ((endTime - startTime) / 1000f));
                publishProgress(currentFrame, counterSeconds, counterLightnings);
                if (openCVHandler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName)) {
                    counterLightnings++;
                }

            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            horizontalprogress.setProgress(0);
            Toast.makeText(getApplicationContext(), "Обработка видео окончена", Toast.LENGTH_LONG).show();
        }

    }

    protected class MediaMetadataRetriever_Decomposing_Task extends AsyncTask<String, Integer, Void> {
        ProgressBar horizontalprogress = (ProgressBar) findViewById(R.id.progressBarFrames);
        TextView textviewCountSeconds = (TextView) findViewById(R.id.textViewCountSeconds);
        TextView textViewCountLightnings = (TextView) findViewById(R.id.textViewFoundedLightnings);
        TextView textViewCountFrames = (TextView) findViewById(R.id.textViewCountFrames);
        ImageView imageView = (ImageView) findViewById(R.id.imageViewCurrentFrame);

        private int durationMs; //длительность видео
        private int frameRate;
        private int frameStep;//специальная переменная для работы getFrameAtTime

        int counterFrames = 0;        //счетчик кадров
        int counterSeconds = 0;       //счетчик обработанных секунд
        int counterLightnings = 0;     //счетчик молний

        //переопределяем конструктор класса для возможности передавать левые аргументы
        public MediaMetadataRetriever_Decomposing_Task(int duration, double frameRateTemp) {
            super();
            durationMs = duration;        //продолжительность видео в миллисекундах
            frameRate = (int) frameRateTemp;  //FPS видео
            frameStep = (int) (1000000 / frameRateTemp); //1000000/FPS - через каждые frameStep микросекунд следует брать новый кадр
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            typeOfTask = 1;
            horizontalprogress.setMax(durationMs / frameRate);
            horizontalprogress.setProgress(0);

            Toast.makeText(getApplicationContext(), "Обработка видео запущена", Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            horizontalprogress.setProgress(progress[0] / frameStep);
            textviewCountSeconds.setText("Секунд видео обработано:" + String.valueOf(progress[1]));
            textViewCountLightnings.setText("Кадров с молниями найдено:" + String.valueOf(progress[2]));
            textViewCountFrames.setText("Кадров обработано:" + String.valueOf(progress[0]));
        }


        @Override
        protected Void doInBackground(String... params) {

            MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
            OpenCVHandler openCVHandler = new OpenCVHandler();

            String videofileName = getFileName(params[0]);
            Bitmap frame;

            //устанавливаем источник для mediadata
            mediaMetadata.setDataSource(params[0]);

            int durationS = durationMs / 1000;    //секундах

            for (int currentFrame = frameStep; currentFrame < durationMs * 1000; currentFrame += frameStep) {
                if (isCancelled()) return null;
                counterFrames++;
                Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame / frameStep);
                if (counterFrames == frameRate) {   //каждые FPS кадров сбрасываем счетчик кадров и добавляем секунду
                    counterFrames = 0;
                    counterSeconds++;
                    Log.d("Lightning Shower Debug:", "Секунда видео: " + counterSeconds);
                }

                long startTime = System.currentTimeMillis();    //засекаем время получения кадра
                frame = mediaMetadata.getFrameAtTime(currentFrame, MediaMetadataRetriever.OPTION_CLOSEST);
                long endTime = System.currentTimeMillis();

                final Bitmap finalBitmapVideoFrame = frame;
                runOnUiThread(new Runnable() {
                    public void run() {
                        imageView.setImageBitmap(finalBitmapVideoFrame);

                    }
                });
                Log.d("Lightning Shower Debug:", "Время выдергивания из видоса: " + ((endTime - startTime) / 1000f));
                if (openCVHandler.preparingBeforeFindContours(frame, currentFrame, videofileName)) {
                    counterLightnings++;
                }
                publishProgress(currentFrame, counterSeconds, counterLightnings);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            horizontalprogress.setProgress(0);
            Toast.makeText(getApplicationContext(), "Обработка видео окончена", Toast.LENGTH_LONG).show();
        }

    }

    //Обработчики кнопок
    public void onClickStopButton(View view) {
        if (typeOfTask == 0) {
            JavaCVTask.cancel(true);
        } else {
            MediaMRetrTask.cancel(true);
        }
    }

}
