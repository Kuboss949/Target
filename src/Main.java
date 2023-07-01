import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
    public static void main(String[] args) {

        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

        String sourceDirectoryPath = "images/";
        String destDirectoryPath = "output/";
        String file = "3.jpg";

        try {
            Mat image = Imgcodecs.imread(sourceDirectoryPath+file);
            Mat grey = new Mat();
            Imgproc.cvtColor(image, grey, Imgproc.COLOR_RGB2GRAY);
            Mat binaryImage = new Mat(grey.rows(), grey.cols(), CvType.CV_8UC1);
            Imgproc.adaptiveThreshold(grey, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 33, 2);
            //Imgcodecs.imwrite(destDirectoryPath+file, binaryImage);
            Mat bluredBinary = new Mat();
            Imgproc.medianBlur(binaryImage, bluredBinary, 5);
            Imgcodecs.imwrite(destDirectoryPath+file, bluredBinary);
            Mat circle = new Mat();
            Imgproc.HoughCircles(bluredBinary, circle, Imgproc.HOUGH_GRADIENT, 1.0, 30.0, 80.0, 300.0, binaryImage.cols()/6, binaryImage.cols());

            double[] cir = circle.get(0, 0);
            double centerX = cir[0];
            double centerY = cir[1];
            double radius = cir[2];

            Imgproc.circle(
                    image,
                    new Point((int) centerX, (int) centerY),
                    (int) radius,
                    new Scalar(0, 0, 255), // BGR color (red in this example)
                    2 // Thickness of the circle's outline
            );

            //Imgcodecs.imwrite(destDirectoryPath+file, image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }
}