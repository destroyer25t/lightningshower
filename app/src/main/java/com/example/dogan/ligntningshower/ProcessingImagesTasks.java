package com.example.dogan.ligntningshower;

/**
 * Created by dogan on 14.04.2016.
 */
public class ProcessingImagesTasks {

    //MediaMetadataRetriever_Decomposing_Task MediaMRetrTask;
    //JavaCVDecomposing_Task JavaCVTask;
    /**
     * AsyncTask класс, реализующий обработку видео с помощью FFMPeg grabber-а
     * Не рекомендуется использовать поскольку андроид гасит выполнение цикла в doInBackground
     */
    /*protected class JavaCVDecomposing_Task extends AsyncTask<String, Integer, Void> {
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

    }*/

    /**
     * AsyncTask класс, реализующий обработку видео с помощью гугловского MediaMetadataRetriever
     * Не рекомендуется использовать поскольку андроид гасит выполнение цикла в doInBackground
     */
    /*protected class MediaMetadataRetriever_Decomposing_Task extends AsyncTask<String, Integer, Void> {
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

    }*/


}
