//TODO:JavaCV пашет прекрасно, но AsyncTask блокируются где то через 20 секунд обработки

package com.example.dogan.ligntningshower;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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

    //счетчики кадров-секунд для runnable версии
    private volatile int framesCounterThrVer = 0;
    private volatile int secondsCounterThrVer = 0;
    private volatile int lightningsCounterThrVer = 0;

    private double frameRateDouble;
    private int frameRate;
    private int durationMs; //длительность видео
    private int durationS;
    private int frames;


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

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videopath);

        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        //инициализируем глобальные значения ФПС, длительности и количества кадров видео
        frameRateDouble = grabber.getFrameRate();
        frameRate = (int) frameRateDouble;
        durationMs = Integer.parseInt(stringDuration);
        durationS = durationMs / 1000;
        frames = grabber.getLengthInFrames();

        if (Objects.equals(typeOfDecomposing, "OPENCVdecomposing")) {
            videoProcessingControl();
            // JavaCVTask = new JavaCVDecomposing_Task();
            //JavaCVTask.execute(videopath);
        } else {
            MediaMRetrTask = new MediaMetadataRetriever_Decomposing_Task();
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

        int counterFrames = 0;        //счетчик кадров
        int counterSeconds = 0;       //счетчик обработанных секунд
        int counterLightnings = 0;     //счетчик молний

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

        private int frameStep;//специальная переменная для работы getFrameAtTime

        int counterFrames = 0;        //счетчик кадров
        int counterSeconds = 0;       //счетчик обработанных секунд
        int counterLightnings = 0;     //счетчик молний

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            frameStep = (int) (1000000 / frameRateDouble); //1000000/FPS - через каждые frameStep микросекунд следует брать новый кадр

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
            textViewCountFrames.setText("Кадров обработано:" + String.valueOf(progress[0] / frameStep));
        }


        @Override
        protected Void doInBackground(String... params) {

            MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
            OpenCVHandler openCVHandler = new OpenCVHandler();

            String videofileName = getFileName(params[0]);
            Bitmap frame;

            //устанавливаем источник для mediadata
            mediaMetadata.setDataSource(params[0]);


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

    protected void videoProcessingControl() {
        ProgressBar horizontalprogress = (ProgressBar) findViewById(R.id.progressBarFrames);
        horizontalprogress.setMax(frames);
        int quarter = frames / 4;
        int rest = frames % 4;
        Thread JavaCVThread1 = new Thread(new JavaCVDecomposing_Thread(1, quarter, videopath));
        Thread JavaCVThread2 = new Thread(new JavaCVDecomposing_Thread(quarter + 1, quarter * 2, videopath));
        Thread JavaCVThread3 = new Thread(new JavaCVDecomposing_Thread(quarter * 2 + 1, quarter * 3, videopath));
        Thread JavaCVThread4 = new Thread(new JavaCVDecomposing_Thread(quarter * 3 + 1, quarter * 4 + rest, videopath));
        JavaCVThread1.start();
        JavaCVThread2.start();
        JavaCVThread3.start();
        JavaCVThread4.start();
    }


    public void refreshUIFunction(final Bitmap img) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar horizontalprogress = (ProgressBar) findViewById(R.id.progressBarFrames);
                TextView textviewCountSeconds = (TextView) findViewById(R.id.textViewCountSeconds);
                TextView textViewCountLightnings = (TextView) findViewById(R.id.textViewFoundedLightnings);
                TextView textViewCountFrames = (TextView) findViewById(R.id.textViewCountFrames);
                ImageView imageView = (ImageView) findViewById(R.id.imageViewCurrentFrame);

                horizontalprogress.setProgress(framesCounterThrVer);
                textviewCountSeconds.setText("Секунд видео обработано:" + String.valueOf(secondsCounterThrVer) + " из " + String.valueOf(durationS));
                textViewCountLightnings.setText("Молний обнаружено:" + String.valueOf(lightningsCounterThrVer));
                textViewCountFrames.setText("Кадров обработано:" + String.valueOf(framesCounterThrVer) + " из " + String.valueOf(frames));
                imageView.setImageBitmap(img);
            }
        });

    }


    protected class JavaCVDecomposing_Thread implements Runnable {
        private int startFrame;
        private int endFrame;
        private String videopath;

        public JavaCVDecomposing_Thread(int startFrame, int endFrame, String videopath) {
            this.startFrame = startFrame;
            this.endFrame = endFrame;
            this.videopath = videopath;
        }

        public void run() {
            Bitmap bitmapVideoFrame;
            Frame videoframe = null;
            int currentFrame = 0;
            String videofileName = getFileName(videopath);
            OpenCVHandler openCVHandler = new OpenCVHandler();

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videopath);
            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();

            try {
                grabber.start();
                grabber.setFrameNumber(startFrame);
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }


            do {
                try {
                    videoframe = grabber.grab();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
                currentFrame = grabber.getFrameNumber();
                framesCounterThrVer++;
                bitmapVideoFrame = converterToBitmap.convert(videoframe);
                final Bitmap finalBitmapVideoFrame = bitmapVideoFrame;

                if (openCVHandler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName)) {
                    lightningsCounterThrVer++;
                }
                refreshUIFunction(finalBitmapVideoFrame);
                Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame);
            } while (currentFrame <= endFrame);
        }

    }


    //Обработчики кнопок
    public void onClickStopButton(View view) {
        if (typeOfTask == 0) {
            JavaCVTask.cancel(true);
            Button buttonStop = (Button) findViewById(R.id.buttonStop);
            buttonStop.setVisibility(View.INVISIBLE);
        } else {
            MediaMRetrTask.cancel(true);
        }
    }

}
