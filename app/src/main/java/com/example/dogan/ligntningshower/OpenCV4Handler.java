package com.example.dogan.ligntningshower;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_xfeatures2d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.CvContour;
import static org.bytedeco.javacpp.opencv_core.CvMemStorage;
import static org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import static org.bytedeco.javacpp.opencv_core.CvRect;
import static org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.Point;
import static org.bytedeco.javacpp.opencv_core.Rect;
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
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;

/**
 * Created by dogan on 18.03.2016
 * commit before transfering to AsyncTask
 */
public class OpenCV4Handler {
/*
    public boolean preparingBeforeFindContours(Bitmap image, int numberOfFrame, String fileOfName) {
        if (image == null) {
            return false;
        }
        Mat img_1;
        Mat img_2;

        if (img_1.empty() || !img_2.empty())
        {
            return false;
        }

        int minHessian = 400;

        opencv_xfeatures2d.SURF detector;
        detector = new opencv_xfeatures2d.SURF();
        detector.setHessianThreshold(minHessian);

       // vector<opencv_core.KeyPoint> keypoints_1, keypoints_2;		//вектора для ключевых точек

        detector.detect(img_2, keypoints_2);


       // SurfDescriptorExtractor extractor;				//-- Шаг 2: вычисляем дескрипторы

        Mat descriptors_1, descriptors_2;				//храним в Mat

        extractor.compute(img_1, keypoints_1, descriptors_1);			//считаем дескрипторы
        extractor.compute(img_2, keypoints_2, descriptors_2);

        //-- Сравниваем вектора дескрипторов при помощи быстрого алгоритма k соседей
        opencv_features2d.FlannBasedMatcher matcher;
        vector<opencv_core.DMatch> matches;
        matcher.match(descriptors_1, descriptors_2, matches);

        double max_dist = 0; double min_dist = 100;

        //-- Ищем минимальную и максимальную дистанцию между ними
        for (int i = 0; i < descriptors_1.rows; i++)
        {
            double dist = matches[i].distance;
            if (dist < min_dist) min_dist = dist;
            if (dist > max_dist) max_dist = dist;
        }

        printf("-- Max dist : %f \n", max_dist);
        printf("-- Min dist : %f \n", min_dist);

        //отрисовываем хорошие точки (расстояние менше чем 2*mindist)
        vector< DMatch > good_matches;

        for (int i = 0; i < descriptors_1.rows; i++)
        {
            if (matches[i].distance <= max(2 * min_dist, 0.02))
            {
                good_matches.push_back(matches[i]);
            }
        }

        Mat img_matches;
        drawMatches(img_1, keypoints_1, img_2, keypoints_2,
                good_matches, img_matches, Scalar::all(-1), Scalar::all(-1),
                vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);

        // Показываем
        imshow("Good Matches", img_matches);

        waitKey(0);

        return 0;
        return false;
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
    */
}

