import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class TargetAnalyzer {
    private String sourceDirectoryPath;
    private String destDirectoryPath;
    private String file;
    public TargetAnalyzer(String file, String source, String dest){
        this.file = file;
        sourceDirectoryPath = source;
        destDirectoryPath = dest;
    }

    public TargetAnalyzer(String file){
        this.file = file;
        sourceDirectoryPath = "images/";
        destDirectoryPath = "output/";
    }

    public boolean checkIfBullsEye(Mat binaryImage, MatOfPoint contour){
        Mat mask = Mat.zeros(binaryImage.size(), CvType.CV_8UC1);
        Imgproc.drawContours(mask, List.of(contour), -1, new Scalar(255), -1);
        Mat masked = new Mat();
        Core.bitwise_and(binaryImage, mask, masked);
        //System.out.println(Core.mean(binaryImage, mask).val[0]);
        return Core.mean(binaryImage, mask).val[0]<95;
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

            double circularityThreshold = 0.85; // Adjust the circularity threshold as needed
            List<MatOfPoint> filteredContours = new ArrayList<>();
            int minContourArea = image.rows()*image.cols()/100;

            for (MatOfPoint contour : contours) {
                double contourArea = Imgproc.contourArea(contour);
                double contourPerimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                double circularity = (4 * Math.PI * contourArea) / (contourPerimeter * contourPerimeter);

                if (circularity >= circularityThreshold && contourArea > minContourArea && checkIfBullsEye(binaryImage, contour)) {
                    filteredContours.add(contour);
                }
            }

            Mat output = new Mat();
            Scalar color = new Scalar(0, 0, 255);
            if(filteredContours.size()>1)
                Imgproc.drawContours(image, List.of(filteredContours.get(filteredContours.size()-1)) , -1, color, 2);
            else
                Imgproc.drawContours(image, filteredContours , -1, color, 2);

            Imgcodecs.imwrite(destDirectoryPath + file, image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
