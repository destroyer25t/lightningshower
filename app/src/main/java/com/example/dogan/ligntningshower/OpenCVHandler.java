package com.example.dogan.ligntningshower;

import android.graphics.Bitmap;
import android.os.Environment;
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
 * Created by dogan on 18.03.2016.
 */
public class OpenCVHandler {
    public IplImage Igray = null, Iat = null, Icolor = null;
    public IplImage initImage = null;
    private int MINAREA = 65000;    //регулируя это задаем величину минимальной площадь контура, после которого он начинает считаться молнией
    private int DOFFSET = 195;    //для демонстрации значение порога задано сразу

    /*static {
        System.loadLibrary("opencv_core");
        System.loadLibrary("jniopencv_core");
    }*/

    private boolean findCountoursDetection(double areaImage, IplImage Iat) {
        CvMemStorage storage = cvCreateMemStorage(0);
        CvSeq contours = new CvSeq();
        CvRect rect = null;
        //находим контуры
        int contoursCont = cvFindContours(Iat, storage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS, cvPoint(0, 0));
        Log.d("TRULALA", Integer.toString(contoursCont));
        //нарисуем контуры
        boolean isdetected = false;
        int counter = 0;
        if (contoursCont >= 0) {
            for (CvSeq seq0 = contours; seq0 != null; seq0 = seq0.h_next()) {
                double area = Math.abs(cvContourArea(seq0));    //площадь контура
                double perim = cvContourPerimeter(seq0);    //периметр контура
                double compact = area / (perim * perim);    //какая то придуманная формула для расчета компактности
                if (compact < 0.01 && area > areaImage && !isdetected) { //area>15
                    double halfper = perim / 1.5;
                    rect = cvBoundingRect(seq0, 0);
                    if ((rect.height() * 3 > Igray.height()) || (halfper > Igray.height()) || (rect.height() > rect.width() * 3))
                        return true;//isdetected = true;
                }
            }

        }//Log.d("TRULALA", "Молния найдена");
        return false;
    }

    void preparingBeforeFindContours(Bitmap image) {
        Igray = IplImage.create(image.getWidth(), image.getHeight(), IPL_DEPTH_8U, 1);
        Icolor = IplImage.create(image.getWidth(), image.getHeight(), IPL_DEPTH_8U, 4);
        //Icolor=cvLoadImage("/mnt/sdcard/20.jpg", CV_LOAD_IMAGE_COLOR));
        //Icolor = cvLoadImage("/mnt/sdcard/20.jpg");
        image.copyPixelsToBuffer(Icolor.getByteBuffer());
        cvCvtColor(Icolor, Igray, CV_RGB2GRAY);
        double areaImage = (Igray.height() * Igray.width()) / MINAREA;

        double offset = DOFFSET;

        //БИНАРИЗАЦИЯ
        Iat = cvCreateImage(cvGetSize(Igray), IPL_DEPTH_8U, 1);
        cvThreshold(Igray, Iat, offset, 255, CV_THRESH_BINARY);
        //Bitmap x = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        //x.copyPixelsFromBuffer(Iat.getByteBuffer());

        //контуры
        if (findCountoursDetection(areaImage, Iat)) {
            saveBitmapToPhone(image);
        }
        ;
    }

    void saveBitmapToPhone(Bitmap image) {
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(file_path);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, "newfile.png");
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


        image.compress(Bitmap.CompressFormat.PNG, 50, fOut);
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

