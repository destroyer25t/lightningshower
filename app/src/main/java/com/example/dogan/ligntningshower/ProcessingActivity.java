package com.example.dogan.ligntningshower;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.example.dogan.ligntningshower.SupportFunctions.generateNotification;
import static com.example.dogan.ligntningshower.SupportFunctions.getFileName;
import static com.example.dogan.ligntningshower.SupportFunctions.getPath;
import static com.example.dogan.ligntningshower.SupportFunctions.killThread;
import static java.lang.Thread.sleep;
import static org.bytedeco.javacv.Parallel.getNumCores;

public class ProcessingActivity extends AppCompatActivity {

    int typeOfTask;     //переменная в которой хранится выбор типа обработки
    Uri imageUri;       //путь URI до видео
    String videopath;   //путь до видео
    SharedPreferences prefs;    //настройки приложения
    float precision;
    boolean isStopped = false;

    //чтобы была возможность убивать потоки и следить за состоянием пришлось вынести в глобал

    private Thread threads[];                           //объявляем массив потоков
    private Thread firstThread;
    private Thread lastThread;
    private Thread controlThread;

    private volatile int numberOfExecutedThreads = 0;     //увеличиваем при каждом завершившемся потоке


    //счетчики кадров-секунд-молний для runnable версии
    private volatile int framesCounterThrVer = 0;
    private volatile int secondsCounterThrVer = 0;
    private volatile int lightningsCounterThrVer = 0;
    private volatile int secondsInMinuteCounterTheVer = 0;


    //все необходимые для работы свойства видео
    private double frameRateDouble;
    private int frameRate;
    private int durationMs; //длительность видео
    private int durationS;
    private int frames;
    private int frameDuration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        //различные инициализации, получение данных из родительского активити
        String tempUri = getIntent().getExtras().getString("imageUri");
        imageUri = Uri.parse(tempUri);  //преобразуем полученный строкой Uri обратно в Uri
        videopath = getPath(this, imageUri);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String tempPrecision = prefs.getString("thresholdOfRecongintion", "0.86");
        precision = Float.parseFloat(tempPrecision);

        try {
            Decomposing(videopath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Функция руководящая процессом обработки видео переданного в активити.
     * Инициализирует все свойства видео
     * В зависимости от выбранного способа обработки запускает соответствующие потоки/задания
     *
     * @param videopath путь к видео
     */
    public void Decomposing(String videopath) throws IOException {

        Button buttonStop = (Button) findViewById(R.id.buttonStop);
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
        frameDuration = durationMs / frames * 1000;
        //frames=durationS*frameRate;

        int numOfCores = getNumCores(); //получаем количество ядер

        ProgressBar horizontalprogress = (ProgressBar) findViewById(R.id.progressBarFrames);
        horizontalprogress.setMax(frames);  //задаем длину прогресс-бара в количество кадров
        controlThread = new Thread(new ThreadControl(numOfCores));

        if (Objects.equals(typeOfDecomposing, "OPENCVdecomposing")) {
            typeOfTask = 0;
            controlThread.start();
        } else {
            typeOfTask = 1;
            controlThread.start();
        }

    }

    /**
     * Класс-поток для контроля остальных классов-потоков
     */

    protected class ThreadControl implements Runnable {
        private int numberOfThreads;
        int quarter = 0;     //делим количество кадров на количество потоков
        int rest = 0;       //получаем остаток в случае если количество кадров не кратно количесву потоков


        public ThreadControl(int numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            if (numberOfThreads > 2)
                threads = new Thread[numberOfThreads - 2];      //инициализируем МАССИВ потоков
            quarter = frames / numberOfThreads;
            rest = frames % numberOfThreads;
        }

        public void run() {

            long startTime = System.currentTimeMillis();    //засекаем время получения кадро

            //в зависимости от выбранного типа обработки typeOfTask инициализируем первый поток и инициализируем последний поток.
            // Первый и последний будут всегда (случай с одним процессором отдельно)...
            if (typeOfTask == 0) {
                firstThread = new Thread(new JavaCVDecomposing_Thread(1, quarter, videopath));

                if (numberOfThreads > 1) {
                    lastThread = new Thread(new JavaCVDecomposing_Thread(quarter * (numberOfThreads - 1) + 1, quarter * numberOfThreads + rest, videopath));

                    //запускаем потоки и инициализируем доп потоки если ядер больше 2
                    firstThread.start();
                    if (numberOfThreads > 2) {
                        for (int i = 1; i < numberOfThreads - 1; i++) {  //numberOfThreads-1 потому что последний поток отдельно. i=1 тоже
                            threads[i - 1] = new Thread(new JavaCVDecomposing_Thread(quarter * i + 1, quarter * (i + 1), videopath));   //i-1 потому что в массиве элементы с 0
                        }

                        for (int i = 1; i < numberOfThreads - 1; i++) {
                            threads[i - 1].start();
                        }
                    }
                    lastThread.start();
                } else {
                    firstThread.start();        //если поток всего один - сразу его и запускаем
                }

            } else {
                //аналогично для второго типа обработки
                firstThread = new Thread(new MediaMetadataRetrDecomposing_Thread(1, quarter, videopath));
                if (numberOfThreads > 1) {
                    lastThread = new Thread(new MediaMetadataRetrDecomposing_Thread(quarter * (numberOfThreads - 1) + 1, quarter * numberOfThreads + rest, videopath));

                    //запускаем потоки
                    firstThread.start();
                    if (numberOfThreads > 2) {
                        for (int i = 1; i < numberOfThreads - 1; i++) {  //numberOfThreads-1 потому что последний поток отдельно. i=1 тоже
                            threads[i - 1] = new Thread(new MediaMetadataRetrDecomposing_Thread(quarter * i + 1, quarter * (i + 1), videopath));   //i-1 потому что в массиве элементы с 0
                        }

                        for (int i = 1; i < numberOfThreads - 1; i++) {
                            threads[i - 1].start();
                        }
                    }
                    lastThread.start();
                } else {
                    firstThread.start();
                }

            }

            while (numberOfExecutedThreads != numberOfThreads) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long endTime = System.currentTimeMillis();
            Log.d("TRULALA", "Время работы потоков: " + ((endTime - startTime) / 1000f));
            refreshUIAfterAll();
            generateNotification(getApplicationContext(), "Видео обработано!", ProcessingActivity.class);


        }


    }

    /**
     * Класс-поток, реализует обработку кадров переданных в конструктор с помощью FFmpeg
     * <p/>
     * startFrame - номер кадра с которого начинается обработка
     * endFrame - номер кадра на котором закончится обработка (включительно)
     * videopath - путь к видео, позже уберу
     */
    protected class JavaCVDecomposing_Thread implements Runnable {
        private int startFrame;
        private int endFrame;
        private String videopath;

        public boolean threadIsDone = false;

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
            OpenCV3Handler openCV3Handler = new OpenCV3Handler();

            int counterFrames = 0;   //счетчик кадров для секунд

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videopath);
            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();

            try {
                grabber.start();
                grabber.setFrameNumber(startFrame);
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }


            while (currentFrame <= endFrame && framesCounterThrVer <= frames && !isStopped) {
                try {
                    videoframe = grabber.grab();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }

                currentFrame = grabber.getFrameNumber();
                secondsInMinuteCounterTheVer++;
                Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame);
                Log.d("Lightning Shower Debug:", "Считаем до секунд: " + secondsInMinuteCounterTheVer);
                if (secondsInMinuteCounterTheVer == frameRate) {   //каждые FPS кадров сбрасываем счетчик кадров и добавляем секунду
                    secondsInMinuteCounterTheVer = 0;
                    secondsCounterThrVer++;
                    Log.d("Lightning Shower Debug:", "Секунда видео: " + secondsCounterThrVer);
                }
                bitmapVideoFrame = converterToBitmap.convert(videoframe);
                framesCounterThrVer++;


                final Bitmap finalBitmapVideoFrame = bitmapVideoFrame;
                // openCV3Handler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName);
                if (openCVHandler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName, precision)) {
                    lightningsCounterThrVer++;
                }
                refreshUIFunction(finalBitmapVideoFrame);


                currentFrame++;
            }
            numberOfExecutedThreads++;
            Log.d("Lightning Shower Debug:", "Поток отработал. Всего потоков отработало: " + numberOfExecutedThreads);

            threadIsDone = true;


        }

    }


    /**
     * Класс-поток, реализует обработку кадров переданных в конструктор с помощью Google API
     * MediaMetadataRetriever
     * <p/>
     * startFrame - номер кадра с которого начинается обработка
     * endFrame - номер кадра на котором закончится обработка (включительно)
     * videopath - путь к видео, позже уберу
     */
    protected class MediaMetadataRetrDecomposing_Thread implements Runnable {
        private int startFrame;
        private int endFrame;
        private String videopath;
        private int frameStep;//специальная переменная для работы getFrameAtTime
        private int frameFirst;//специальная переменная для работы getFrameAtTime
        private int currentFrame = 0; //номер текущего кадра который обрабатывается

        public boolean threadIsDone = false;


        MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
        OpenCVHandler openCVHandler = new OpenCVHandler();
        OpenCV3Handler openCV3Handler = new OpenCV3Handler();
        String videofileName;
        Bitmap frame;

        public MediaMetadataRetrDecomposing_Thread(int startFrame, int endFrame, String videopath) {
            //устанавливаем источник для mediadata
            mediaMetadata.setDataSource(videopath);
            frameStep = (int) (1000000 / frameRateDouble); //1000000/FPS - через каждые frameStep микросекунд следует брать новый кадр
            frameFirst = (int) (10000000 / frameRateDouble);
            this.startFrame = frameStep * startFrame;
            // this.endFrame = frameStep * endFrame;
            this.endFrame = frameDuration * endFrame;
            this.videopath = videopath;
            this.videofileName = getFileName(videopath);


        }

        public void run() {

            int counterFrames = 0;
            for (currentFrame = startFrame; currentFrame < endFrame; currentFrame += frameStep) {
                if (isStopped) {
                    break;
                } else {
                    secondsInMinuteCounterTheVer++;
                    Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame / frameStep);
                    if (secondsInMinuteCounterTheVer == frameRate) {   //каждые FPS кадров сбрасываем счетчик кадров и добавляем секунду
                        secondsInMinuteCounterTheVer = 0;
                        secondsCounterThrVer++;
                        Log.d("Lightning Shower Debug:", "Секунда видео: " + secondsCounterThrVer);
                    }

                    long startTime = System.currentTimeMillis();    //засекаем время получения кадра
                    frame = mediaMetadata.getFrameAtTime(currentFrame, MediaMetadataRetriever.OPTION_CLOSEST);
                    long endTime = System.currentTimeMillis();

                    Log.d("Lightning Shower Debug:", "gFAT: " + currentFrame);

                    final Bitmap finalBitmapVideoFrame = frame;

                    Log.d("Lightning Shower Debug:", "Время выдергивания из видоса: " + ((endTime - startTime) / 1000f));
                    //openCV3Handler.preparingBeforeFindContours(frame, currentFrame, videofileName);
                    if (openCVHandler.preparingBeforeFindContours(frame, currentFrame, videofileName, precision)) {
                        lightningsCounterThrVer++;
                    }
                    refreshUIFunction(finalBitmapVideoFrame);
                    framesCounterThrVer++;
                }

            }
            numberOfExecutedThreads++;
            Log.d("Lightning Shower Debug:", "Поток отработал. Всего потоков отработало: " + numberOfExecutedThreads);

            threadIsDone = true;
        }

    }

    //------------------------------------------------------------------------------------------
    //Обработчики кнопок


    public void onClickStopButton(View view) {
        isStopped = true;
        killAllThreads();
    }

    @Override
    public void onBackPressed() {
        if (numberOfExecutedThreads == getNumCores()) {
            super.onBackPressed();
        } else {
            openQuitDialog();
        }

    }

    private void killAllThreads() {
        //сначала проверяем два потока которые точно должны быть
        if (firstThread.isAlive()) {
            killThread(firstThread);

            if (getNumCores() > 1) {
                //если запущен первый поток возможно запущены и другие, поэтому проверяем только после первого
                for (Thread i : threads) {
                    if (i.isAlive()) killThread(i);
                }
                if (lastThread.isAlive()) killThread(lastThread);
            }

        }

        refreshUIAfterAll();

    }


    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                ProcessingActivity.this);
        quitDialog.setTitle("Вы уверены? Процесс обработки будет остановлен");

        quitDialog.setPositiveButton("Да", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                killAllThreads();
                finish();
            }
        });

        quitDialog.setNegativeButton("Нет", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        quitDialog.show();
    }

    /**
     * Функция позволяющая обновлять элементы интерфейса. Также в нее передается Bitmap
     *
     * @param img для обновления ImageView - эскиз текущего кадра
     */
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

    public void refreshUIAfterAll() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button buttonStop = (Button) findViewById(R.id.buttonStop);
                buttonStop.setVisibility(View.INVISIBLE);
            }
        });
    }
}
