package com.pfa;

import org.opencv.core.Mat;
import static java.lang.Math.abs;

/**
 * Determine the error of the computed disparity using various compariason methods.
 */
public class DisparityError {
    /**
     * average difference between trueDisparity and computedDisparity value
     */
    double averageError;
    /**
     * same as averageError, but with absolute values
     */
    double absAverageError;
    /**
     * error considering the case where trueDisparity values = computedDisparity + k, k constant
     */
    double errorIfConstSum;
    /**
     * error considering the case where trueDisparity values = computedDisparity * k, k constant
     */
    double errorIfConstMult;
    /**
     * error calculated with disparityErrorByBlock method
     */
    double avgBlocksError;

    /**
     * calculate potential errors in the disparity map established
     * @param trueDisparity the true disparity matrix from the dataset
     * @param computedDisparity the disparity matrix computed from the left image from the dataset
     */
    public DisparityError(Mat trueDisparity, Mat computedDisparity){
        int rows = trueDisparity.rows();
        int cols = trueDisparity.cols();
        disparityErrorByBlock(trueDisparity, computedDisparity, rows, cols);

        averageError = 0;
        absAverageError = 0;
        errorIfConstSum = 0;
        errorIfConstMult = 0;
        double avgMultCoef = 0;
        int n = 0;
        byte[] trueDispArray = new byte[rows*cols];
        byte[] compDispArray = new byte[rows*cols];
        computedDisparity.get(0, 0, compDispArray);
        trueDisparity.get(0, 0, trueDispArray);
        for (int x = 0 ; x < rows ; x++){
            for (int y = 0 ; y < cols ; y++){
                averageError += trueDispArray[y + cols * x] - compDispArray[y + cols * x];
                absAverageError += abs(trueDispArray[y + cols * x] - compDispArray[y + cols * x]);
                if (trueDispArray[y + cols * x] != 0){
                    avgMultCoef += (double) compDispArray[y + cols * x]/trueDispArray[y + cols * x];
                    n++;
                }
            }
        }
        averageError /= rows * cols;
        absAverageError /= rows * cols;
        avgMultCoef /= n;

        for (int x = 0 ; x < rows ; x++){
            for (int y = 0 ; y < cols ; y++){
                errorIfConstSum += abs((trueDispArray[y + cols * x] - compDispArray[y + cols * x]) - averageError);
                errorIfConstMult += abs(trueDispArray[y + cols * x]*avgMultCoef-compDispArray[y + cols * x]);
            }
        }
        errorIfConstSum /= rows*cols;
        errorIfConstMult /= rows*cols;

        if (errorIfConstSum < 1){
            System.out.println("constant shift error of " + averageError);
        } else if(errorIfConstMult < 1){
            System.out.println("constant multiplicative error of " + avgMultCoef);
        } else {
            System.out.println("average error : " + absAverageError);
        }
    }

    /**
     * another method to calculate potential errors in the disparity map established
     * cut the disparity maps in blocs and calculate the difference between the average values of each block
     * @param trueDisparity the true disparity matrix from the dataset
     * @param computedDisparity the disparity matrix computed from the left image from the dataset
     */
    private void disparityErrorByBlock(Mat trueDisparity, Mat computedDisparity, int rows, int cols){

        int sizeBlockRows = rows/20;
        int sizeBlockCols = cols/20;
        int nBlockRows = rows / sizeBlockRows;
        int nBlockCols = cols / sizeBlockCols;
        byte[] trueDispArray = new byte[rows*cols];
        byte[] compDispArray = new byte[rows*cols];
        trueDisparity.get(0, 0, trueDispArray);
        computedDisparity.get(0, 0, compDispArray);

        avgBlocksError = 0;
        float avgBlockTrueDispValue;
        float avgBlockCompDispValue;

        for (int i = 0 ; i < rows - sizeBlockRows ; i += sizeBlockRows){
            for (int j = 0 ; j < cols - sizeBlockCols ; j += sizeBlockCols){
                avgBlockTrueDispValue = 0;
                avgBlockCompDispValue = 0;
                for (int x = i ; x < i + sizeBlockRows ; x++){
                    for (int y = j ; y < j + sizeBlockCols ; y++){
                        avgBlockTrueDispValue += trueDispArray[y + cols * x];
                        avgBlockCompDispValue += compDispArray[y + cols * x];
                    }
                }
                avgBlockTrueDispValue /= sizeBlockRows * sizeBlockCols;
                avgBlockCompDispValue /= sizeBlockRows * sizeBlockCols;
                avgBlocksError += abs(avgBlockTrueDispValue - avgBlockCompDispValue);
            }
        }
        avgBlocksError /= nBlockRows * nBlockCols;
    }
}
