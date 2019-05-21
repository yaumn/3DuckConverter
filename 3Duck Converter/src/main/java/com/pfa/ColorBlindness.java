package com.pfa;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Correcter and simulater for daltonism
 */
public class ColorBlindness
{
    /**
     * All the matrix used for applying color blindess filters
     */

    private final Mat m_rgb2lms = new Mat(3, 3, CvType.CV_32F);
    private final Mat m_lms2rgb = new Mat(3, 3, CvType.CV_32F);

    private final Mat m_protanopia = new Mat(3, 3, CvType.CV_32F);
    private final Mat m_deuteranopia = new Mat(3, 3, CvType.CV_32F);
    private final Mat m_tritanopia = new Mat(3, 3, CvType.CV_32F);

    private final Mat m_protanopiaShifting = new Mat(3, 3, CvType.CV_32F);
    private final Mat m_deuteranopiaShifting = new Mat(3, 3, CvType.CV_32F);
    private final Mat m_tritanopiaShifting = new Mat(3, 3, CvType.CV_32F);


    public enum Type
    {
        Protanopia,
        Deuteranopia,
        Tritanopia
    }


    public ColorBlindness()
    {
        double data[] = new double[] {0.31399022, 0.63951294, 0.04649755,
                                      0.15537241, 0.75789446, 0.08670142,
                                      0.01775239, 0.10944209, 0.87256922};
        m_rgb2lms.put(0, 0, data);

        data = new double[] {5.47221206, -4.6419601, 0.16963708,
                             -1.1252419, 2.29317094, -0.1678952,
                             0.02980165, -0.19318073, 1.16364789};
        m_lms2rgb.put(0, 0, data);

        data = new double[] {0.0, 1.05118294, -0.05116099,
                             0.0, 1.0, 0.0,
                             0.0, 0.0, 1.0};
        m_protanopia.put(0, 0, data);

        Core.gemm(m_protanopia, m_rgb2lms, 1, new Mat(), 0, m_protanopia);
        Core.gemm(m_lms2rgb, m_protanopia, 1, new Mat(), 0, m_protanopia);

        data = new double[] {1.0, 0.0, 0.0,
                             0.9513092, 0.0, 0.04866992,
                             0.0, 0.0, 1.0};
        m_deuteranopia.put(0, 0, data);

        Core.gemm(m_deuteranopia, m_rgb2lms, 1, new Mat(), 0, m_deuteranopia);
        Core.gemm(m_lms2rgb, m_deuteranopia, 1, new Mat(), 0, m_deuteranopia);

        data = new double[] {1.0, 0.0, 0.0,
                             0.0, 1.0, 0.0,
                             -0.86744736, 1.86727089, 0.0};
        m_tritanopia.put(0, 0, data);

        Core.gemm(m_tritanopia, m_rgb2lms, 1, new Mat(), 0, m_tritanopia);
        Core.gemm(m_lms2rgb, m_tritanopia, 1, new Mat(), 0, m_tritanopia);

        data = new double[] {0.0, 0.0, 0.0,
                             0.7, 1.0, 0.0,
                             0.7, 0.0, 1.0};
        m_protanopiaShifting.put(0, 0, data);

        data = new double[] {1.0, 0.7, 0.0,
                             0.0, 0.0, 0.0,
                             0.0, 0.7, 1.0};
        m_deuteranopiaShifting.put(0, 0, data);

        data = new double[] {1.0, 0.0, 0.7,
                             0.0, 1.0, 0.7,
                             0.0, 0.0, 0.0};
        m_tritanopiaShifting.put(0, 0, data);
    }


    /**
     * Simulate color blindess on an image
     * @param src  input image to simulate
     * @param dst  output image where the simulated image will be stored
     * @param type  type of color blindness to simulate
     */
    public void simulate(Mat src, Mat dst, Type type)
    {
        switch (type) {
            case Protanopia:
                Core.transform(src, dst, m_protanopia);
                break;

            case Deuteranopia:
                Core.transform(src, dst, m_deuteranopia);
                break;

            case Tritanopia:
                Core.transform(src, dst, m_tritanopia);
                break;
        }
    }


    /**
     * Correct color blindness on an image
     * @param src  input image to correct
     * @param dst  output image where the corrected image will be stored
     * @param type  type of color blindness to correct
     */
    public void correct(Mat src, Mat dst, Type type)
    {
        Mat m = new Mat();
        simulate(src, m, type);

        Core.subtract(src, m, m);

        switch (type) {
            case Protanopia:
                Core.transform(m, dst, m_protanopiaShifting);
                break;

            case Deuteranopia:
                Core.transform(m, dst, m_deuteranopiaShifting);
                break;

            case Tritanopia:
                Core.transform(m, dst, m_tritanopiaShifting);
                break;
        }

        Core.add(src, dst, dst);
    }
}
