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

    MediaMetadataRetriever_Decomposing_Task MediaMRetrTask;
    JavaCVDecomposing_Task JavaCVTask;

    int typeOfTask;     //переменная в которой хранится выбор типа обработки
    Uri imageUri;       //путь URI до видео
    String videopath;   //путь до видео
    SharedPreferences prefs;    //настройки приложения
    float precision;

    private static final int NOTIFY_ID = 13395;   //id нашего уведомления о выполнении поиска

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
     * AsyncTask класс, реализующий обработку видео с помощью FFMPeg grabber-а
     * Не рекомендуется использовать поскольку андроид гасит выполнение цикла в doInBackground
     */
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
                if (openCVHandler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName, precision)) {
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

    /**
     * AsyncTask класс, реализующий обработку видео с помощью гугловского MediaMetadataRetriever
     * Не рекомендуется использовать поскольку андроид гасит выполнение цикла в doInBackground
     */
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
                if (openCVHandler.preparingBeforeFindContours(frame, currentFrame, videofileName, precision)) {
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

    /**
     * Класс-поток для контроля остальных классов-потоков
     */

    protected class ThreadControl implements Runnable {
        private int numberOfThreads;
        int quarter = 0;     //делим количество кадров на количество потоков
        int rest = 0;       //получаем остаток в случае если количество кадров не кратно количесву потоков


        public ThreadControl(int numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            threads = new Thread[numberOfThreads - 2];      //инициализируем МАССИВ
            quarter = frames / numberOfThreads;
            rest = frames % numberOfThreads;

        }

        public void run() {

            long startTime = System.currentTimeMillis();    //засекаем время получения кадро

            //в зависимости от выбранного типа обработки typeOfTask...
            if (typeOfTask == 0) {
                firstThread = new Thread(new JavaCVDecomposing_Thread(1, quarter, videopath)); //запускаем первый поток
                //задаем потоки между первым и последним
                if (numberOfThreads > 2) {
                    for (int i = 1; i < numberOfThreads - 1; i++) {  //numberOfThreads-1 потому что последний поток отдельно. i=1 тоже
                        threads[i - 1] = new Thread(new JavaCVDecomposing_Thread(quarter * i + 1, quarter * (i + 1), videopath));   //i-1 потому что в массиве элементы с 0
                    }
                }
                //запускаем последний поток. Первый и последний будут всегда (мы не используем количество потоков меньше 2)
                lastThread = new Thread(new JavaCVDecomposing_Thread(quarter * (numberOfThreads - 1) + 1, quarter * numberOfThreads + rest, videopath));


                //запускаем потоки
                firstThread.start();
                if (numberOfThreads > 2) {
                    for (int i = 1; i < numberOfThreads - 1; i++) {
                        threads[i - 1].start();
                    }
                }
                lastThread.start();
            } else {
                firstThread = new Thread(new MediaMetadataRetrDecomposing_Thread(1, quarter, videopath)); //запускаем первый поток
                //задаем потоки между первым и последним
                if (numberOfThreads > 2) {
                    for (int i = 1; i < numberOfThreads - 1; i++) {  //numberOfThreads-1 потому что последний поток отдельно. i=1 тоже
                        threads[i - 1] = new Thread(new MediaMetadataRetrDecomposing_Thread(quarter * i + 1, quarter * (i + 1), videopath));   //i-1 потому что в массиве элементы с 0
                    }
                }
                //запускаем последний поток. Первый и последний будут всегда (мы не используем количество потоков меньше 2)
                lastThread = new Thread(new MediaMetadataRetrDecomposing_Thread(quarter * (numberOfThreads - 1) + 1, quarter * numberOfThreads + rest, videopath));


                //запускаем потоки
                firstThread.start();
                if (numberOfThreads > 2) {
                    for (int i = 1; i < numberOfThreads - 1; i++) {
                        threads[i - 1].start();
                    }
                }
                lastThread.start();
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
            generateNotification(getApplicationContext(), "Найдена куча молний!", ProcessingActivity.class);


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

            int counterFrames = 0;   //счетчик кадров для секунд

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
                if (openCVHandler.preparingBeforeFindContours(bitmapVideoFrame, currentFrame, videofileName, precision)) {
                    lightningsCounterThrVer++;
                }
                refreshUIFunction(finalBitmapVideoFrame);


                currentFrame++;
            } while (currentFrame <= endFrame && framesCounterThrVer <= frames);

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
        private int currentFrame = 0; //номер текущего кадра который обрабатывается


        MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
        OpenCVHandler openCVHandler = new OpenCVHandler();

        String videofileName;
        Bitmap frame;

        public MediaMetadataRetrDecomposing_Thread(int startFrame, int endFrame, String videopath) {
            //устанавливаем источник для mediadata
            mediaMetadata.setDataSource(videopath);
            frameStep = (int) (1000000 / frameRateDouble); //1000000/FPS - через каждые frameStep микросекунд следует брать новый кадр

            this.startFrame = frameStep * startFrame;
            this.endFrame = frameStep * endFrame;
            this.videopath = videopath;
            this.videofileName = getFileName(videopath);


        }

        public void run() {

            int counterFrames = 0;
            for (currentFrame = startFrame; currentFrame < endFrame; currentFrame += frameStep) {

                counterFrames++;
                Log.d("Lightning Shower Debug:", "Кадр видео: " + currentFrame / frameStep);
                if (counterFrames == frameRate) {   //каждые FPS кадров сбрасываем счетчик кадров и добавляем секунду
                    counterFrames = 0;
                    secondsCounterThrVer++;
                    Log.d("Lightning Shower Debug:", "Секунда видео: " + secondsCounterThrVer);
                }

                long startTime = System.currentTimeMillis();    //засекаем время получения кадра
                frame = mediaMetadata.getFrameAtTime(currentFrame, MediaMetadataRetriever.OPTION_CLOSEST);
                long endTime = System.currentTimeMillis();

                final Bitmap finalBitmapVideoFrame = frame;

                Log.d("Lightning Shower Debug:", "Время выдергивания из видоса: " + ((endTime - startTime) / 1000f));
                if (openCVHandler.preparingBeforeFindContours(frame, currentFrame, videofileName, precision)) {
                    lightningsCounterThrVer++;
                }
                refreshUIFunction(finalBitmapVideoFrame);
                framesCounterThrVer++;
            }

        }

    }

    //Обработчики кнопок
    /*
    public void onClickStopButton(View view) {
        if (typeOfTask == 0) {
            JavaCVTask.cancel(true);
            Button buttonStop = (Button) findViewById(R.id.buttonStop);
            buttonStop.setVisibility(View.INVISIBLE);
        } else {
            MediaMRetrTask.cancel(true);
        }
    }
    */
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

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        openQuitDialog();
    }

    private void killAllThreads() {
        //сначала проверяем два потока которые точно должны быть
        if (firstThread.isAlive()) {
            killThread(firstThread);

            //если запущен первый поток возможно запущены и другие, поэтому проверяем только после первого
            for (Thread i : threads) {
                if (i.isAlive()) killThread(i);
            }
        }
        if (lastThread.isAlive()) killThread(lastThread);


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
}
