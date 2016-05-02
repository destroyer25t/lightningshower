package com.example.dogan.ligntningshower;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by dogan on 18.03.2016
 * commit before transfering to AsyncTask
 */
public class OpenCV3Handler {
    ArrayList<CvSeq> contoursList;
    ArrayList<Rect> rectsList;

    private void findCountoursDetection(double scaleCoeff, IplImage Iat, IplImage Igray) {
        CvMemStorage storage = cvCreateMemStorage(0);
        CvMemStorage storageApprox = cvCreateMemStorage(0);

        CvSeq contours = new CvSeq();
        CvRect rect;
        Rect kek;

        //находим контуры
        //long startTime = System.currentTimeMillis();    //засекаем время получения кадро
        int contoursCont = cvFindContours(Iat, storage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS, cvPoint(0, 0));
        cvApproxPoly(contours, sizeof(CvContour.class), storageApprox, 0, 3, 1);
        //long endTime = System.currentTimeMillis();
        //Log.d("TRULALA", "Время работы cvFindContours: " + ((endTime - startTime) / 1000f));

        int counter = 0;

        if (contoursCont >= 0) {
            for (CvSeq seq0 = contours; seq0 != null; seq0 = seq0.h_next()) {
                rect = cvBoundingRect(seq0, 0);
                kek = new Rect(rect);
                int rectArea = rect.height() * rect.width();

                if (rectArea > scaleCoeff * 20) {
                    contoursList.add(seq0);
                    rectsList.add(kek);
                }
            }
        }

        storage.deallocate();
        contours.deallocate();
    }

    private void processingCorners(IplImage Igray, double scaleCoeff) {
        Mat src_gray = new Mat(Igray);
        Mat corners = new Mat();

        int max_corners = 200;
        //opencv_core.CvPoint2D32f cornersPoints = new opencv_core.CvPoint2D32f(max_corners);
        double qualityLevel = 0.01;
        double minDistance = scaleCoeff / 2;

        CvPoint2D32f cornersPoints = new CvPoint2D32f(max_corners);
        //CvArr cvarr1.
        IntPointer intPointer = new IntPointer(max_corners);
        cvGoodFeaturesToTrack(Igray, Igray, Igray, cornersPoints, intPointer, qualityLevel, minDistance);
/*
        goodFeaturesToTrack(src_gray,
                corners,
                max_corners,
                qualityLevel,
                minDistance);
*/
        Log.d("Lightning Shower Debug:", "Capacity intPointer -  " + intPointer.capacity());
        ArrayList<Point> arrayList = new ArrayList<Point>(intPointer.capacity());


        //for (int i = 0; i < rectsList.size(); i++) {
        //  Rect rect = rectsList.get(i);
        //}

    }

    public boolean preparingBeforeFindContours(Bitmap image, int numberOfFrame, String fileOfName) {
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

        findCountoursDetection(scaleCoeff, Iat, Igray);
        Log.d("Lightning Shower Debug:", "GOVNO");
        processingCorners(Igray, scaleCoeff);
        //контуры

        //saveBitmapToPhone(image, fileOfName, numberOfFrame);
        //Igray.deallocate();
        // Iat.deallocate();
        //Icolor.deallocate();
        //return true;
        //} else {
            Igray.deallocate();
            Iat.deallocate();
            Icolor.deallocate();
            return false;
        // }
    }

    void saveBitmapToPhone(Bitmap image, String fileOfName, int counter) {
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies";
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

