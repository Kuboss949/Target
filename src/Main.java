import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static java.lang.Math.abs;

public class Main {
    public static void main(String[] args) {

        for(int i=10; i<22; i++){
            TargetAnalyzer t = new TargetAnalyzer(i+".jpg", 2);
            t.findBullsEye();
        }


    }
}