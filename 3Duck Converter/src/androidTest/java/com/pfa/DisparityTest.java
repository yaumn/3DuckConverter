package com.pfa;

import org.junit.Test;
import org.opencv.core.Mat;

import static org.junit.Assert.*;

/**
 * Check for a set number of images the error of its disparity
 */
public class DisparityTest {

    static public String disparityIsCorrect(int n) {
        Mat leftImage = ImageConversion.loadImage(Parameters.testLeftImageFolder + n + ".jpg");
        Mat trueDisp = ImageConversion.loadDisparity(Parameters.testDisparityFolder + n + ".jpg");
        Disparity2 computedDisp = new Disparity2();
        computedDisp.init(leftImage);
        computedDisp.computeDisparity(0);
        DisparityError disparityError = new DisparityError(trueDisp, computedDisp.heatMap);
        String s = new String("Image " + n + "\n\tabsError : " + disparityError.absAverageError +
                "\n\tsumError : " + disparityError.errorIfConstSum +
                "\n\tmultError : " + disparityError.errorIfConstMult + "\n");
        System.out.format("pfa::test::disparityError " + s);
        return s;
    }

    static public String testDisparity() {
        String s = new String();
        for (int n = 1; n <= Parameters.testedDisparityNumber; n++) {
            s += disparityIsCorrect(n);
        }
        return s;
    }

}
