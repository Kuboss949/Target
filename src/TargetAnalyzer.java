import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.round;

public class TargetAnalyzer{
    private String sourceDirectoryPath;
    private String destDirectoryPath;
    private String file;
    int shootCount;
    int[] bullsEye = new int[3];

    Point center;
    float radius;
    float outerRadius;
    float shotRadius;


    public TargetAnalyzer(String file, String source, String dest){
        this.file = file;
        sourceDirectoryPath = source;
        destDirectoryPath = dest;
    }

    public TargetAnalyzer(String file, int shootCount){
        this.file = file;
        sourceDirectoryPath = "images/";
        destDirectoryPath = "output/";
        this.shootCount = shootCount;
    }

    enum detectionMode{
        BULLSEYE,
        SHOT
    }

    public boolean checkIfBullsEye(Mat binaryImage, MatOfPoint contour, detectionMode mode){
        Mat mask = Mat.zeros(binaryImage.size(), CvType.CV_8UC1);
        Imgproc.drawContours(mask, List.of(contour), -1, new Scalar(255), -1);
        Mat masked = new Mat();
        Core.bitwise_and(binaryImage, mask, masked);
       // System.out.println(Core.mean(binaryImage, mask).val[0]);
        return mode==detectionMode.BULLSEYE ? Core.mean(binaryImage, mask).val[0]<95 : Core.mean(binaryImage, mask).val[0]>110;
    }

    public List<MatOfPoint> findRings(Mat binaryImage, List<MatOfPoint> contours, double circularityThreshold, int areaCoef,detectionMode mode){
        List<MatOfPoint> filteredContours = new ArrayList<>();
        int minContourArea = binaryImage.rows()*binaryImage.cols()/areaCoef;

        for (MatOfPoint contour : contours) {
            double contourArea = Imgproc.contourArea(contour);
            double contourPerimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double circularity = (4 * Math.PI * contourArea) / (contourPerimeter * contourPerimeter);

            if (circularity >= circularityThreshold && contourArea > minContourArea && checkIfBullsEye(binaryImage, contour, mode)) {
                filteredContours.add(contour);
            }
        }
        return filteredContours;
    }

    public void findBullsEye(){
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        try {
            Mat image = Imgcodecs.imread(sourceDirectoryPath + file);
            Mat grey = new Mat();
            Imgproc.cvtColor(image, grey, Imgproc.COLOR_RGB2GRAY);
            Mat binaryImage = new Mat(grey.rows(), grey.cols(), CvType.CV_8UC1);
            Imgproc.adaptiveThreshold(grey, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 33, 2);
            List<MatOfPoint> contours= new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

            List<MatOfPoint> filteredContours = findRings(binaryImage, contours, 0.6, 100, detectionMode.BULLSEYE);


            center = new Point();
            float[] radiuss = new float[1];


            if(filteredContours.size()==0)
                System.out.println("Circle not found " + file);
            else{
                if(filteredContours.size()>1)
                    Imgproc.minEnclosingCircle(new MatOfPoint2f(filteredContours.get(filteredContours.size()-1).toArray()), center, radiuss);
                else
                    Imgproc.minEnclosingCircle(new MatOfPoint2f(filteredContours.get(0).toArray()), center, radiuss);

                radius = radiuss[0];

                //Imgproc.circle(image, center, (int) radius, new Scalar(0, 255, 0), 2);

                outerRadius = 22.75f*radius/15.25f-2;
                shotRadius = 2.25f*radius/15.25f;
                findShots(image);
            }

            //Imgcodecs.imwrite(destDirectoryPath + file, image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void findShots(Mat image){
        Rect roi = new Rect((int)(this.center.x-this.outerRadius-12), (int)(this.center.y-this.outerRadius-12), 2*((int)this.outerRadius+12), 2*((int)this.outerRadius+12));
        Mat target = new Mat(image, roi);

        /* TAK KONWERTOWAĆ I ZAPISYWAĆ W ORYGINALE

        Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_GRAY2BGR);
        grayImage.copyTo(image.submat(roi));*/


        Mat grey = new Mat();
        Imgproc.cvtColor(target, grey, Imgproc.COLOR_RGB2GRAY);
        Mat binaryImage = new Mat(grey.rows(), grey.cols(), CvType.CV_8UC1);
        //Imgproc.adaptiveThreshold(grey, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 33, 1);

        //Mat blur = new Mat();
        //Imgproc.medianBlur(grey, blur, 21);

        Imgproc.threshold(grey, binaryImage, 180, 255, Imgproc.THRESH_BINARY);
        //Imgproc.cvtColor(binaryImage, binaryImage, Imgproc.COLOR_GRAY2BGR);
        int kernelSize = 3;
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        Imgproc.morphologyEx(binaryImage, binaryImage, Imgproc.MORPH_OPEN, kernel);

        /*Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int connectivity = 4; // 8-way connectivity
        Imgproc.connectedComponentsWithStats(binaryImage, labels, stats, centroids, connectivity, CvType.CV_32S);
        Imgproc.cvtColor(binaryImage, binaryImage, Imgproc.COLOR_GRAY2BGR);
        // Iterate through the connected components
        for (int i = 1; i < stats.rows(); i++) { // Start from 1 to skip the background component
            int x = (int) stats.get(i, Imgproc.CC_STAT_LEFT)[0];
            int y = (int) stats.get(i, Imgproc.CC_STAT_TOP)[0];
            int width = (int) stats.get(i, Imgproc.CC_STAT_WIDTH)[0];
            int height = (int) stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];

            // Draw bounding box around the component
            Imgproc.rectangle(binaryImage, new Point(x, y), new Point(x + width, y + height), new Scalar(0, 255, 0), 2);
        }*/


        List<MatOfPoint> contours= new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

        //List<MatOfPoint> filteredContours = findRings(binaryImage, contours, 0.0, 100, detectionMode.SHOT);
        List<MatOfPoint> filteredContours =new ArrayList<>();
        double imgArea = binaryImage.width()*binaryImage.height();


        System.out.println("Image " + file + "size: " + imgArea);
        for ( var contour: contours) {
            double area = Imgproc.contourArea(contour);
            if(contour.isContinuous() && area<imgArea*0.08 && area > imgArea*0.0009){
                System.out.println("Contour area: " + area);
                filteredContours.add(contour);
            }
        }


        Imgproc.cvtColor(binaryImage, binaryImage, Imgproc.COLOR_GRAY2BGR);

        Imgproc.drawContours(binaryImage, filteredContours, -1, new Scalar(255,0,0), 2);
        if(filteredContours.size()<shootCount && filteredContours.size() !=0){
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(filteredContours.get(0).toArray()));
            Point[] boxPoints = new Point[4];
            rect.points(boxPoints);

            List<Point> boxList = new ArrayList<>();
            boxList.addAll(Arrays.asList(boxPoints));

            List<MatOfPoint> boxContours = new ArrayList<>();
            boxContours.add(new MatOfPoint());
            boxContours.get(0).fromList(boxList);

            Imgproc.drawContours(binaryImage, boxContours, 0, new Scalar(0, 0, 255), 2);
        }


        binaryImage.copyTo(image.submat(roi));




        Imgcodecs.imwrite(destDirectoryPath + file, image);
    }
}
