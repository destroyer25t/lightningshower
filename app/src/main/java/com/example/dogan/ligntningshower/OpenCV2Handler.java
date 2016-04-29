package com.example.dogan.ligntningshower;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by dogan on 18.03.2016
 * commit before transfering to AsyncTask
 */
public class OpenCV2Handler {

    MatVector contours;

    ArrayList<ArrayList<Point>> contoursVec;    //вектор содержащий все контуры после findContRect
    ArrayList<ArrayList<Point>> goodContours;   //вектор содержащий только векторы отсеянные по размеру
    ArrayList<Rect> boundRect;                    //вектор содержащий обрисовывающие прямоугольники
    ArrayList<ArrayList<Point2f>> goodPoints;    //угловые точки которые лежат в прямугольниках


    /*
    Реализация через поиск замкнутых контуров с помощью опенцвшной функции cvFindCountors.
    Функция возвращает булевое значение - есть ли молния на картинке или нет. Критерий тупой и недопиленный - проверяем
    есть ли среди отсеянных по отношению площадь/периметр^2 и имеющих площадь больше 10 контуров - те, у которых
    периметр/1.5>чем высота картинки
    */

    void findContRect(Mat binarizedImage, Mat grayImage, Mat originalImage, int scaleCoeff) {
        Scalar color;
        color = new Scalar(0, 0, 255.0, 0);

        findContours(binarizedImage, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

        /// Approximate contours to polygons + get bounding rects and circles
        Mat contours_poly = new Mat();
        // ArrayList<ArrayList<Point> > contours_poly = new ArrayList<ArrayList<Point>>((int) contours.size());
        Rect tempRect;
        int areaOfTempRect = 0;

        Mat contoursVec = new Mat();
        contours .8
        contoursVec.
        for (int i = 0; i < contours.size(); i++) {
            approxPolyDP(contours, contours_poly, 3, true);    //аппроксимируем контур более простым контуром
            tempRect = boundingRect(Mat(contours_poly[i]));                //рисуем вокруг него обр. прям.
            areaOfTempRect = tempRect.width() * tempRect.height();            //вычисляем площадь
            if (areaOfTempRect > scaleCoeff * 20) {                        //отсеиваем все слишком маленькое относительно картинки
                goodContours.add(contours_poly);                //впихиваем контур в вектор хороших контуров
                boundRect.add(tempRect);                            //впихиваем обр. прям. в вектор обр.прям.
            }

        }

    }

    /**
     * @function goodFeaturesToTrack_Demo.cpp
     * @brief Apply Shi-Tomasi corner detector
     */
    void goodFeaturesToTrack_Demo(Mat src, Mat src_gray, int maxCorners, int scaleCoof) {

        /// Parameters for Shi-Tomasi algorithm
        Mat corners = new Mat();
        //ArrayList<Point2f> corners = new ArrayList<Point2f>();
        double qualityLevel = 0.01;
        double minDistance = scaleCoof / 2;
        int blockSize = 3;
        boolean useHarrisDetector = false;
        double k = 0.04;

        /// Copy the source image
        Mat copy;
        copy = src.clone();
        /// Apply corner detection
        goodFeaturesToTrack(src_gray,
                corners,
                maxCorners,
                qualityLevel,
                minDistance,
                new Mat(),
                blockSize,
                useHarrisDetector,
                k);


        /// Рисуем и впихиваем подходящие угловые точки в вектор соответствующих угловых точек
        int r = 4;
        for (int j = 0; j < boundRect.size(); j++) {
            ArrayList<Point2f> tempVectorCorners;
            int i = 0;
            while (i < corners.cols()) {
                Point2f point = corners.pop_back();

            }
            for (int i = 0; i < corners.cols(); i++) {

                if (boundRect.get(j).contains(corners.cols(i))) {
                    tempVectorCorners.add(corners.);
                }

            }
            goodPoints.add(tempVectorCorners);
        }
    }

    boolean checkLightnings(ArrayList<ArrayList<Point>> contoursPoints, ArrayList<ArrayList<Point2f>> cornersPoints, int scaleCoeff) {

        for (int i = 0; i < cornersPoints.size(); i++) {
            ArrayList<Point> tempVectorForOutput = new ArrayList<Point>();//вектор для zone[i] содержащий точки контура =contoursPoints[i]
            ArrayList<Point2f> tempVectorCornersForOutput = new ArrayList<Point2f>();    //вектор для zone[i] содержащий угловые точки =cornersPoints[i]

            ArrayList<Integer> weightCorners = new ArrayList<Integer>();                                        //вектор весов для угловых точек - сколько точек контура рядом с угловой точкой
            int overlapPoints = 0;

            if (tempVectorCornersForOutput.size() > 1) {                    //отсеиваем зоны в которой меньше двух угловых точек
                for (int j = 0; j < tempVectorCornersForOutput.size(); j++) {            //циклом по всем угловым точкам зоны
                    Point2f cornerPoint = tempVectorCornersForOutput.get(j);                //вытаскиваем угловую точку

                    int weight = 0;

                    for (int k = 0; k < tempVectorForOutput.size(); k++) {            //для каждой угловой точки цикл по всем контурным точкам в zone[i]

                        Point contourPoint = tempVectorForOutput.get(k);                //вытаскиваем контурную точку
                        float xDiff = Math.abs(contourPoint.x() - cornerPoint.x());            //считаем расстояние по x и y до каждой контурной точки
                        float yDiff = Math.abs(contourPoint.y() - cornerPoint.y());

                        //cout << xDiff << "  " << yDiff << endl;
                        int radius = scaleCoeff / 7;                                //некоторая граница зависящая от размера картинка, попадание в которую значит то что угловая точка лежит рядом с основным контуром
                        if ((xDiff < radius) || (yDiff < radius)) {
                            weight++;                                                //если попала - увеличиваем вес контурной точки
                        }
                    }

                    if (weight > 2) {
                        weightCorners.add(weight);                            //впихиваем в вектор весов контурных точек
                    }
                }
                if (weightCorners.size() > 4) {
                    return true;
                }

            }
        }
        return false;
    }


    public boolean preparingBeforeFindContours(Bitmap image, int numberOfFrame, String fileOfName, float precision) {
        if (image == null) {
            return false;
        }

        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        Mat originalImage, grayImage, forApproximingImage, binarizedImage;

        int DOFFSET = 195;    //для демонстрации значение порога задано сразу

        // Загрузка изображение в чёрно- белом формате
        originalImage = converterToMat.

        //определяем площадь картинки в пикселях
        int areaOfImage = originalImage.rows() * originalImage.cols();

        //создаем копию в серых оттенках
        cvtColor(originalImage, grayImage, CV_BGR2GRAY);
        cvtColor(originalImage, forApproximingImage, CV_BGR2GRAY);
        cvtColor(originalImage, binarizedImage, CV_BGR2GRAY);

        int scaleCoeff = originalImage.rows() / 20;
        double offset = DOFFSET;

        //БИНАРИЗАЦИЯ
        threshold(grayImage, binarizedImage, offset, 255, 0);

        //ПОИСК КОНТУРОВ И УГЛОВЫХ ТОЧЕК
        findContRect(binarizedImage, grayImage, originalImage, scaleCoeff);
        goodFeaturesToTrack_Demo(originalImage, grayImage, 200, scaleCoeff);

        //ПРОВЕРКА ТОЧЕК БЛИЖАЙШИХ К УГЛОВЫМ
        if (checkLightnings(goodContours, goodPoints, scaleCoeff)) {
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

