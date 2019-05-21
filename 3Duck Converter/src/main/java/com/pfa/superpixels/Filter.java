/**
 * Dead code: is not used at all in the project
 */

package com.pfa.superpixels;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


/**
 * @Deprecated
 */
@Deprecated
public class Filter
{
    static Mat smooth(Mat img, double sigma)
    {
        Mat dst = new Mat();
        Imgproc.GaussianBlur(img, dst, new Size(0, 0), sigma, sigma);
        return dst;
    }
}
