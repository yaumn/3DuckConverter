package com.pfa;

import android.os.Environment;

import java.io.File;

/**
 * Useful parameters of the application
 */
public class Parameters {

    /**
     * Define the maximum number of row to compute a disparity map
     * if realTime is true.
     * If the input image is bigger, a resized image will be used then
     * the disparity map will be resized to fit the input image.
     */
    static int maxHeatmapRow = 200;
    /**
     * Abstract representation of the number of large superpixels desired
     */
    static int maxLargeSuperpixelNumber = 15;
    /**
     * Abstract representation of the number of small superpixels desired
     */
    static int maxSmallSuperpixelNumber = 25;
    /**
     * Threshold to define if two color are considered approximatly equals
     */
    static int sameColorThreshold = 40;
    /**
     * If true save each time the computed disparity
     */
    static boolean saveDisparity = true;
    /**
     * If true save each time the resulting image
     */
    static boolean saveImage = true;
    /**
     * If true cap the maximum size to compute the disparity map to
     * enhance the performances
     */
    static boolean realTime = true;
    /**
     * Minimum percentage of the pixels considered as background in a line
     * to define the limit between the ground and the sky
     */
    static int groundSkyFindLimit = 10;
    /**
     * Scale the computed disparity to optimize the result
     */
    static double disparityScaling = 0.075;

    /**
     * Root of the repository used by the application
     */
    static String rootFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PFA/";

    static String testFolder =  "Test/";
    static String testDisparityFolder = testFolder + "Disparity/";
    static String testLeftImageFolder = testFolder + "LeftImage/";
    static String testLogFolder = testFolder + "Log/";
    static String testLog = testLogFolder + "log.txt";

    /**
     * Number of image tested automatically
     */
    private static File rep = new File(Parameters.rootFolder + testDisparityFolder);
    static int testedDisparityNumber = 0;
    static {
        if (rep.list() != null) {
            testedDisparityNumber = rep.list().length;
        }
    }
}
