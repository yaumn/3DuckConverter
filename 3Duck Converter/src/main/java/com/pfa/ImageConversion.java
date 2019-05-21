package com.pfa;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Aggregate of conversion's methods for an image
 */
public class ImageConversion
{
    /**
     * Save the image on the phone (for testing only)
     * The image will be saved in the <DIRECTORY_PICTURES>/PFA directory
     * @param img   the image to save (in RGB format)
     * @param filename   the name of the image
     * @return true if the image was successfully saved, false otherwise
     */
    static boolean saveImage(Mat img, String filename)
    {
        Mat dst = new Mat(img.size(), img.type());
        Imgproc.cvtColor(img, dst, Imgproc.COLOR_RGB2BGR);
        return Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PFA/" + filename, dst);
    }


    /**
     * Save the disparity image on the phone (for testing only)
     * The image will be saved in the <DIRECTORY_PICTURES>/PFA directory
     * @param img  the disparity to save (in grayscale format)
     * @param filename  the name of the disparity image
     * @return true if the image was successfully saved, false otherwise
     */
    static boolean saveDisparity(Mat img, String filename)
    {
        Mat dst = img.clone();
        Imgproc.cvtColor(img, dst, Imgproc.COLOR_GRAY2BGR);
        Log.d("PFA::CONV", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        return Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PFA/" + filename, dst);
    }


    /**
     * Load an image from the phone (for testing only)
     * The image must be located in the <DIRECTORY_PICTURES>/PFA directory
     * @param filename   the name of the image to load
     * @return  the loaded image
     */
    static Mat loadImage(String filename)
    {
        return Imgcodecs.imread(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PFA/" + filename, Imgcodecs.IMREAD_COLOR);
    }


    /**
     * Load a disparity image from the phone (for testing only)
     * The image must be located in the <DIRECTORY_PICTURES>/PFA directory
     * @param filename   the name of the image to load
     * @return  the loaded image
     */
    static Mat loadDisparity(String filename)
    {
        return Imgcodecs.imread(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PFA/" + filename, Imgcodecs.IMREAD_GRAYSCALE);
    }


    /**
     * Convert two images (left and right view) into a red-cyan anaglyph image
     * The left and right images must have the same size
     * @param left  the left-view image
     * @param right  the right-view image
     * @param dest  the matrix where the anaglyph image will be stored
     */
    static void convertToAnaglyph(Mat left, Mat right, Mat dest)
    {
        dest.create(left.rows(), left.cols(), left.type());

        List<Mat> leftChannels = new ArrayList<>(3);
        List<Mat> rightChannels = new ArrayList<>(3);
        List<Mat> destChannels = new ArrayList<>(3);

        Core.split(left, leftChannels);
        Core.split(right, rightChannels);

        destChannels.add(leftChannels.get(0));
        destChannels.add(rightChannels.get(1));
        destChannels.add(rightChannels.get(2));

        Core.merge(destChannels, dest);
    }


    /**
     * Convert two images (left and right view) into a side-by-side image
     * The left and right images must have the same size
     * @param left  the left-view image
     * @param right  the right-view image
     * @param dest  the matrix where the side-by-side image will be stored
     */
    static void convertToSideBySide(Mat left, Mat right, Mat dest)
    {
        dest.create(left.rows(), left.cols() + right.cols(), left.type());

        left.copyTo(dest.submat(0, left.rows(), 0, left.cols()));
        right.copyTo(dest.submat(0, right.rows(), left.cols(), left.cols() + right.cols()));
    }


    static Mat heatmapConversion(CameraBridgeViewBase.CvCameraViewFrame inputFrame, Disparity2 disparity) {
        //Mat load = loadImage("UntouchedImage.jpg");
        disparity.init(inputFrame);
        disparity.computeDisparity(40);
        if (Parameters.saveDisparity)
            saveDisparity(disparity.heatMap, "disparity_heatmap.jpg");
        return disparity.heatMap;
    }


    /**
     * Perform a full 3D conversion of the given image img
     * @param img  the image to convert into 3D
     * @param dst  the image where the 3D image will be stored
     * @param conversionType  a string representing the type of 3D, either "Anaglyph" or "Side by side"
     * @return  the columns that were cropped from the input image comparing to the output image
     */
    static int full3DConversion(Mat img, Mat dst, String conversionType)
    {
        if (conversionType.equals("None")) {
            img.copyTo(dst);
            return 0;
        }

        int rows = img.rows();
        int cols = img.cols();

        dst.create(rows, cols, img.type());

        Disparity2 disparity = new Disparity2();
        disparity.init(img);
        disparity.computeDisparity(40);

        Mat right = new Mat();
        int scale = (int)(255.0 / (cols * Parameters.disparityScaling));
        if (scale == 0){
            scale = 1;
        }

        int biggestEdgeDisparity = ImageConversion.computeCorrespondingImage(img, disparity.heatMap,
                right, true, scale);
        int rightRows = right.rows();
        int rightCols = right.cols();
        System.out.format("pfa::conv right %dx%d type %d\n", rightRows, rightCols, right.type());
        System.out.format("pfa::conv Img %dx%d type %d\n", img.rows(), img.cols(), img.type());
        if (conversionType.equals("Anaglyph")) {
            Mat anaglyph = new Mat(rightRows, rightCols, right.type());
            ImageConversion.convertToAnaglyph(img.submat(0, rows,
                    0, cols - biggestEdgeDisparity), right, anaglyph);
            anaglyph.copyTo(dst.submat(0, rightRows, biggestEdgeDisparity / 2,
                    rightCols + biggestEdgeDisparity / 2));
        } else {
            Mat sbs = new Mat(rightRows, rightCols, right.type());
            ImageConversion.convertToSideBySide(img.submat(0, rows,
                    rightCols / 4, cols - biggestEdgeDisparity - rightCols / 4),
                    right.submat(0, rightRows, rightCols / 4, rightCols - rightCols / 4), sbs);
            sbs.copyTo(dst.submat(0, sbs.rows(),  biggestEdgeDisparity / 2,
                    sbs.cols() + biggestEdgeDisparity / 2));
        }

        if (Parameters.saveImage) {
            ImageConversion.saveImage(dst, "computed_image.jpg");
        }

        return biggestEdgeDisparity;
    }


    /**
     * Perform a full 3D conversion of the given image img (for testing only)
     * In this function, the disparity map is just a matrix where each pixel has the same value
     * @param img  the image to convert into 3D
     * @param dst  the image where the 3D image will be stored
     * @param conversionType  a string representing the type of 3D, either "Anaglyph", "Side by side" or "None"
     * @return  the columns that were cropped from the input image comparing to the output image
     */
    static int full3DConversionDummy(Mat img, Mat dst, String conversionType)
    {
        int rows = img.rows();
        int cols = img.cols();

        dst.create(rows, cols, img.type());

        Mat dummyDisparity = new Mat(rows, cols, CvType.CV_8UC1);
        dummyDisparity.setTo(new Scalar(20));

        Mat right = new Mat();
        int biggestEdgeDisparity = ImageConversion.computeCorrespondingImage(img, dummyDisparity,
                right, true, 1);
        int rightRows = right.rows();
        int rightCols = right.cols();

        System.out.format("pfa::im %d\n", biggestEdgeDisparity);

        if (conversionType.equals("Anaglyph")) {
            Mat anaglyph = new Mat(rightRows, rightCols, right.type());
            ImageConversion.convertToAnaglyph(img.submat(0, rows,
                    0, cols - biggestEdgeDisparity), right, anaglyph);
            anaglyph.copyTo(dst.submat(0, rightRows, biggestEdgeDisparity / 2,
                    rightCols + biggestEdgeDisparity / 2));
        } else {
            Mat sbs = new Mat(rightRows, rightCols, right.type());
            ImageConversion.convertToSideBySide(img.submat(0, rows,
                    rightCols / 4, cols - biggestEdgeDisparity - rightCols / 4),
                    right.submat(0, rightRows, rightCols / 4, rightCols - rightCols / 4), sbs);
            sbs.copyTo(dst.submat(0, sbs.rows(),  biggestEdgeDisparity / 2,
                    sbs.cols() + biggestEdgeDisparity / 2));
        }

        return biggestEdgeDisparity;
    }


    /**
     * Add the pixel value to the pixel sum rgb (used for extrapolation)
     * @param pixels  the image buffer containing all the pixels of the image
     * @param index   the index of the pixel to add to the sum
     * @param rgb  the current pixels sum
     * @param surroundingPixelsNb  the number of pixel added to the sum so far
     */
    static void addSurroundingPixel(byte pixels[], int index, int rgb[], int surroundingPixelsNb[])
    {
        rgb[0] += pixels[index] < 0 ? 256 + pixels[index] : pixels[index];
        rgb[1] += pixels[index + 1] < 0 ? 256 + pixels[index + 1] : pixels[index + 1];
        rgb[2] += pixels[index + 2] < 0 ? 256 + pixels[index + 2] : pixels[index + 2];
        ++surroundingPixelsNb[0];
    }


    /**
     * Extrapolate a pixel value from its neighbors
     * The function computes the average of the neighbors (maximum 8 pixels) of a pixel
     * and assigns the value to the pixel
     * @param dest  the image buffer containing all the pixels of the image
     * @param rows  the number of rows of the image
     * @param cols  the number of columns of the image
     * @param step  the step of the image
     * @param i  the i coordinate of the pixel to extrapolate
     * @param j  the j coordinate of the pixel to extrapolate
     * @param computedPixels  array of booleans indicating which pixels were already computed
     */
    static void extrapolatePixelValue(byte[] dest, int rows, int cols, int step,
                                      int i, int j, boolean computedPixels[][])
    {
        int rgb[] = new int[3];
        int surroundingPixelsNb[] = new int[1];

        // Check all the surrounding pixels
        if (i > 0 && computedPixels[i - 1][j]) {
            addSurroundingPixel(dest, step * (i - 1) + j * 3, rgb, surroundingPixelsNb);
        }
        if (i < rows - 1 && computedPixels[i + 1][j]) {
            addSurroundingPixel(dest, step * (i + 1) + j * 3, rgb, surroundingPixelsNb);
        }
        if (j > 0 && computedPixels[i][j - 1]) {
            addSurroundingPixel(dest, step * i + (j - 1) * 3, rgb, surroundingPixelsNb);
        }
        if (j < cols - 1 && computedPixels[i][j + 1]) {
            addSurroundingPixel(dest, step * i + (j + 1) * 3, rgb, surroundingPixelsNb);
        }
        if (i > 0 && j > 0 && computedPixels[i - 1][j - 1]) {
            addSurroundingPixel(dest, step * (i - 1) + (j - 1) * 3, rgb, surroundingPixelsNb);
        }
        if (i > 0 && j < cols - 1 && computedPixels[i - 1][j + 1]) {
            addSurroundingPixel(dest, step * (i - 1) + (j + 1) * 3, rgb, surroundingPixelsNb);
        }
        if (j > 0 && i < rows - 1 && computedPixels[i + 1][j - 1]) {
            addSurroundingPixel(dest, step * (i + 1) + (j - 1) * 3, rgb, surroundingPixelsNb);
        }
        if (i < rows - 1 && j < cols - 1 && computedPixels[i + 1][j + 1]) {
            addSurroundingPixel(dest, step * (i + 1) + (j + 1) * 3, rgb, surroundingPixelsNb);
        }

        if (surroundingPixelsNb[0] == 0) {
            return;
        }

        // Calculate extrapolated pixel value
        dest[i * step + j * 3] = (byte)(rgb[0] / surroundingPixelsNb[0]);
        dest[i * step + j * 3 + 1] = (byte)(rgb[1] / surroundingPixelsNb[0]);
        dest[i * step + j * 3 + 2] = (byte)(rgb[2] / surroundingPixelsNb[0]);

        computedPixels[i][j] = true;
    }


    /**
     * Extrapolate all pixels that do not have a value after the disparity was applied
     * @param dest  the image buffer containing all the pixels of the image
     * @param rows  the number of rows of the image
     * @param cols  the number of columns of the image
     * @param step  the step of the image
     * @param computedPixels  array of booleans indicating which pixels were already computed
     * @param leftInput  boolean indicating whether the input image was the left-view or right-view image
     */
    static void handleOcclusionsWithSurroundingPixelsAverage(byte[] dest, int rows, int cols, int step,
                                                             boolean computedPixels[][], boolean leftInput)
    {
        // If there is an occlusion on the computed right-view, then
        // the value of the extrapolated pixel is more likely to be
        // similar to the pixel to its right than to its left.
        // Therefore we extrapolate pixels from the right to the left.
        // And vice-versa if we compute the left-view.

        if (leftInput) {
            for(int i = 0 ; i < rows ; ++i) {
                for (int j = cols - 1; j >= 0 ; j--) {
                    if (!computedPixels[i][j]) {
                        extrapolatePixelValue(dest, rows, cols, step, i, j, computedPixels);
                    }
                }
            }
        } else {
            for(int i = 0 ; i < rows ; ++i) {
                for (int j = 0 ; j < cols ; j++) {
                    if (!computedPixels[i][j]) {
                        extrapolatePixelValue(dest, rows, cols, step, i, j, computedPixels);
                    }
                }
            }
        }
    }


    /**
     * Compute the image corresponding to the other view of the input image (i.e. left or right)
     * @param img  the image whose other view is to be computed
     * @param disparity  the disparity map corresponding to the input image
     * @param dest  the matrix where the computed image will be stored
     * @param leftInput  boolean indicating whether the input image is the left-view or the right-view
     * @param disparityScale  number by which each disparity value must be divided
     * @return  the columns that were cropped from the input image comparing to the output image
     */
    static int computeCorrespondingImage(Mat img, Mat disparity, Mat dest,
                               boolean leftInput, int disparityScale)
    {
        int shiftDirection = leftInput ? -1 : 1;
        //Mat dest = new Mat();
        Mat tmp = new Mat(img.rows(), img.cols(), img.type());
        //dest.create(img.rows(), img.cols(), img.type());

        int biggestEdgeDisparity = 255; // Used to crop the image

        // Matrix where if a cell is false, it means that the corresponding pixel on
        // the computed image has no equivalent in the input image (occlusion)
        // Therefore it should be extrapolated later
        boolean computedPixels[][] = new boolean[img.rows()][img.cols()];

        byte disparity_buffer[] = new byte[(int)(disparity.total() * disparity.channels())];
        disparity.get(0, 0, disparity_buffer);

        int imgChannels = img.channels();

        byte imgBuffer[] = new byte[(int)(img.total() * imgChannels)];
        img.get(0, 0, imgBuffer);

        byte tmpBuffer[] = new byte[(int)(tmp.total() * tmp.channels())];
        tmp.get(0, 0, tmpBuffer);

        int dispStep = (int)disparity.step1();
        int imgStep = (int)img.step1();
        int tmpStep = (int)tmp.step1();

        int rows = img.rows();
        int cols = img.cols();


        // Apply disparity to each pixel of the input image
        for (int i = 0 ; i < rows ; i++) {
            for (int j = 0 ; j < cols ; j++) {
                int d = disparity_buffer[dispStep * i + j];
                if (d < 0) {
                    d += 256;
                }
                int computedColumn = j + shiftDirection * (d / disparityScale);

                // No need to consider pixels which would be outside of the image's bounds
                if (d >= 0 && computedColumn >= 0 && computedColumn < cols) {
                    for (int k = 0 ; k < imgChannels ; k++) {
                        tmpBuffer[tmpStep * i + computedColumn * imgChannels + k]
                                = imgBuffer[imgStep * i + j * imgChannels + k];
                        computedPixels[i][computedColumn] = true;
                    }
                }

                if (((leftInput && j == img.cols() - 1) || (!leftInput && j == 0))
                        && biggestEdgeDisparity < d) {
                    biggestEdgeDisparity = d;
                }
            }
        }


        tmp.put(0, 0, tmpBuffer);

        // Crop image from left or right (depending whether the computed image
        // is the left view or the right view)
        biggestEdgeDisparity /= disparityScale;
        dest.create(tmp.rows(), tmp.cols() - biggestEdgeDisparity, tmp.type());
        
        if (leftInput) {
            tmp.submat(0, tmp.rows(), 0, tmp.cols() - biggestEdgeDisparity).copyTo(dest);
        } else {
            tmp.submat(0, tmp.rows(), biggestEdgeDisparity, tmp.cols()).copyTo(dest);
        }


        byte destBuffer[] = new byte[(int)(dest.total() * dest.channels())];
        dest.get(0, 0, destBuffer);

        handleOcclusionsWithSurroundingPixelsAverage(destBuffer, dest.rows(), dest.cols(), (int)dest.step1(),
                computedPixels, leftInput);


        dest.put(0, 0, destBuffer);

        return biggestEdgeDisparity;
    }
}