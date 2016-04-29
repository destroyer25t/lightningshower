package com.example.dogan.ligntningshower;

import android.graphics.Bitmap;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.CvBox2D;
import static org.bytedeco.javacpp.opencv_core.CvContour;
import static org.bytedeco.javacpp.opencv_core.CvMemStorage;
import static org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_LINK_RUNS;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvBoundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvMinAreaRect2;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_imgproc.goodFeaturesToTrack;

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

    private void processingCorners(IplImage Igray, int scaleCoeff) {
        Mat src_gray = new Mat(Igray);
        Mat corners = new Mat();

        int max_corners = 200;
        //opencv_core.CvPoint2D32f cornersPoints = new opencv_core.CvPoint2D32f(max_corners);
        double qualityLevel = 0.01;
        double minDistance = scaleCoeff / 2;

        // cvGoodFeaturesToTrack(Igray, Igray, Igray,cornersPoints, max_corners, qualityLevel,minDistance, mask, blockSize,useHarrisDetector,k );

        goodFeaturesToTrack(src_gray,
                corners,
                max_corners,
                qualityLevel,
                minDistance);
        ArrayList<Float> arrayList = new ArrayList<>(corners.size());
        if (corners.isContinuous()) {
            arrayList.
        }
        for (int i = 0; i < rectsList.size(); i++) {
            Rect rect = rectsList.get(i);
        }

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
        if (findCountoursDetection(scaleCoeff, Iat, Igray)) {
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

