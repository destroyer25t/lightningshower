package com.example.dogan.ligntningshower;

import android.graphics.Bitmap;

import static org.bytedeco.javacpp.Loader.sizeof;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by dogan on 18.03.2016.
 */
public class OpenCVHandler {
    private IplImage Igray = null, Iat = null;
    private IplImage initImage = null;
    private int MINAREA= 65000;	//регулируя это задаем величину минимальной площадь контура, после которого он начинает считаться молнией
    private int DOFFSET= 195;	//для демонстрации значение порога задано сразу

    private void findCountoursDetection(double areaImage) {
        CvMemStorage storage = cvCreateMemStorage(0);
        CvSeq contours = null;
        CvRect rect=null;
        //находим контуры
        int contoursCont = cvFindContours(Iat, storage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS, cvPoint(0, 0));
        //нарисуем контуры
        boolean isdetected = false;
        int counter = 0;
        for (CvSeq seq0 = contours; seq0 != null; seq0 = seq0.h_next()) {
            double area = Math.abs(cvContourArea(seq0));	//площадь контура
            double perim = cvContourPerimeter(seq0);	//периметр контура
            double compact = area / (perim*perim);	//какая то придуманная формула для расчета компактности
            if (compact < 0.01&&area>areaImage) { //area>15
                double halfper = perim / 1.5;
                rect = cvBoundingRect(seq0, 0);
                if ((rect.height() * 3 > Igray.height()) || (halfper > Igray.height()) || (rect.height()>rect.width() * 3)) isdetected = true;

            }
        }
    }

    void preparingBeforeFindContours(Bitmap image){
        Igray = IplImage.create(image.getWidth(),image.getHeight(), IPL_DEPTH_8U, 1);
        image.copyPixelsToBuffer(Igray.getByteBuffer());
        cvCvtColor(initImage, Igray, CV_RGB2GRAY);
        double areaImage = (Igray.height()*Igray.width())/MINAREA;

        double offset = DOFFSET;

        //БИНАРИЗАЦИЯ
        Iat = cvCreateImage(cvGetSize(Igray), IPL_DEPTH_8U, 1);
        cvThreshold(Igray, Iat, offset, 255, CV_THRESH_BINARY);

        //контуры
        findCountoursDetection(areaImage);
    }
}
