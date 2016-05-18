package com.example.dogan.ligntningshower;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by dogan on 18.03.2016
 * commit before transfering to AsyncTask
 */
public class OpenCVHandler {

    private boolean findCountoursDetection(double scaleCoeff, IplImage Iat, float precision) {
        CvMemStorage storage = cvCreateMemStorage(0);
        CvSeq contours = new CvSeq();
        CvBox2D box;
        //находим контуры
        //long startTime = System.currentTimeMillis();    //засекаем время получения кадро
        int contoursCont = cvFindContours(Iat, storage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS, cvPoint(0, 0));
        //long endTime = System.currentTimeMillis();
        //Log.d("TRULALA", "Время работы cvFindContours: " + ((endTime - startTime) / 1000f));

        int counter = 0;

        if (contoursCont >= 0) {
            for (CvSeq seq0 = contours; seq0 != null; seq0 = seq0.h_next()) {
                box = cvMinAreaRect2(seq0, storage);
                double area = Math.abs(cvContourArea(seq0));    //площадь контура
                double perim = cvContourPerimeter(seq0);    //периметр контура
                double compact = area / (perim * perim);    //какая то придуманная формула для расчета компактности
                float proportion;
                if (box.size().width() > box.size().height()) {
                    proportion = box.size().height() / (float) box.size().width();
                } else {
                    proportion = box.size().width() / (float) box.size().height();
                }

                if (compact < 0.01 && area > scaleCoeff * 9) { //area>15
                    if (proportion <= precision) {
                        box.deallocate();
                        storage.deallocate();
                        contours.deallocate();
                        return true;
                    }

                }
            }

        }
        storage.deallocate();
        contours.deallocate();
        return false;
    }

    public boolean preparingBeforeFindContours(Bitmap image, int numberOfFrame, String fileOfName, float precision) {
        if (image == null) {
            return false;
        }
        IplImage Igray = null, Iat = null, Icolor = null;
        int DOFFSET = 195;    //для демонстрации значение порога задано сразу


        Igray = IplImage.create(image.getWidth(), image.getHeight(), IPL_DEPTH_8U, 1);
        Icolor = IplImage.create(image.getWidth(), image.getHeight(), IPL_DEPTH_8U, 4);
        image.copyPixelsToBuffer(Icolor.getByteBuffer());
        cvCvtColor(Icolor, Igray, CV_RGB2GRAY);
        double scaleCoeff = Igray.height() / 20;

        double offset = DOFFSET;

        //БИНАРИЗАЦИЯ
        Iat = cvCreateImage(cvGetSize(Igray), IPL_DEPTH_8U, 1);
        cvThreshold(Igray, Iat, offset, 255, CV_THRESH_BINARY);

        //контуры
        if (findCountoursDetection(scaleCoeff, Iat, precision)) {
            saveBitmapToPhone(image, fileOfName, numberOfFrame);
            Igray.deallocate();
            Iat.deallocate();
            Icolor.deallocate();
            return true;
        } else {
            Igray.deallocate();
            Iat.deallocate();
            Icolor.deallocate();
            return false;
        }
    }

    void saveBitmapToPhone(Bitmap image, String fileOfName, int counter) {
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies"; //папка где хранятся видосы
        file_path = file_path + File.separator + fileOfName;
        File dir = new File(file_path);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, fileOfName + "_" + counter + ".jpg");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        image.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

