package com.pfa;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.ximgproc.SuperpixelSLIC;
import org.opencv.ximgproc.Ximgproc;

/**
 * Handles camera activities
 */
public class CameraActivity extends Activity implements CvCameraViewListener2
{
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mProcessedFrame = new Mat();
    private Disparity2 disparity = new Disparity2();
    private ImageConversion imageConverter= new ImageConversion();
    private ColorBlindness colorBlindness = new ColorBlindness();
    private int frame = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_camera);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        String frameSize = getIntent().getStringExtra("frameSize");
        if (!frameSize.isEmpty()) {
            int i = frameSize.indexOf('x');
            mOpenCvCameraView.setMaxFrameSize(Integer.valueOf(frameSize.substring(0, i)),
                    Integer.valueOf(frameSize.substring(i + 1)));
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mOpenCvCameraView.enableView();
    }


    public void onCameraViewStarted(int width, int height)
    { }


    public void onCameraViewStopped()
    { }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        frame++;
        Mat rgbSrc = new Mat();
        Mat dst = new Mat();
        Imgproc.cvtColor(inputFrame.rgba(), rgbSrc, Imgproc.COLOR_RGBA2RGB);
        int rows = rgbSrc.rows();
        int cols = rgbSrc.cols();


        if (getIntent().getStringExtra("mode").equals("Normal")) {
            ImageConversion.full3DConversion(rgbSrc, dst, getIntent().getStringExtra("3d"));
        } else if (getIntent().getStringExtra("mode").equals("Segmentation")) {
            dst = rgbSrc;
            int regionSize = rows / Parameters.maxLargeSuperpixelNumber;
            SuperpixelSLIC sup = Ximgproc.createSuperpixelSLIC(rgbSrc, Ximgproc.MSLIC, regionSize, (float) 0.075f);
            sup.iterate(1);
            sup.enforceLabelConnectivity(50);

            Mat mask = new Mat();
            sup.getLabelContourMask(mask, true);
            dst.setTo(new Scalar(0, 0, 255), mask);
        } else if (getIntent().getStringExtra("mode").equals("Heat Map")) {
            return ImageConversion.heatmapConversion(inputFrame, disparity);
        } else {
                Mat image3D = new Mat();

                int biggestEdgeDisparity = ImageConversion.full3DConversion(rgbSrc, image3D,
                        getIntent().getStringExtra("3d"));
                int image3DCols = image3D.cols() - biggestEdgeDisparity;

                ColorBlindness.Type type;
                if (getIntent().getStringExtra("colorBlindness").equals("Protanopia")) {
                    type = ColorBlindness.Type.Protanopia;
                } else if (getIntent().getStringExtra("colorBlindness").equals("Deuteranopia")) {
                    type = ColorBlindness.Type.Deuteranopia;
                } else {
                    type = ColorBlindness.Type.Tritanopia;
                }


                dst.create(rows, cols, rgbSrc.type());

                if (getIntent().getStringExtra("mode").equals("Correct")) {
                    if (getIntent().getStringExtra("3d").equals("Anaglyph")) {
                        dst = image3D;
                    } else {
                        colorBlindness.correct(image3D, dst, type);
                    }
                } else if (getIntent().getStringExtra("mode").equals("Simulate")) {
                    Mat simulated = new Mat();
                    dst = image3D;

                    if (getIntent().getStringExtra("3d").equals("Side by side")) {
                        colorBlindness.simulate(image3D.submat(0, rows,
                                biggestEdgeDisparity / 2 + image3DCols / 4, biggestEdgeDisparity / 2 + image3DCols / 2),
                                simulated, type);
                        simulated.copyTo(dst.submat(0, rows, biggestEdgeDisparity / 2 + image3DCols / 4,
                                biggestEdgeDisparity / 2 + image3DCols / 2));

                        colorBlindness.simulate(image3D.submat(0, rows,
                                biggestEdgeDisparity / 2 + image3DCols / 2 + image3DCols / 4,
                                biggestEdgeDisparity / 2 + image3DCols),
                                simulated, type);
                        simulated.copyTo(dst.submat(0, rows, biggestEdgeDisparity / 2 + image3DCols / 2 + image3DCols / 4,
                                biggestEdgeDisparity / 2 + image3DCols));
                    } else if (getIntent().getStringExtra("3d").equals("None")) {
                        colorBlindness.simulate(image3D.submat(0, rows, image3DCols / 2, image3DCols),
                                simulated, type);
                        simulated.copyTo(dst.submat(0, rows, image3DCols / 2, image3DCols));
                    }
                }
            }


        dst.copyTo(mProcessedFrame);
        rgbSrc.release();
        dst.release();

        return mProcessedFrame;
    }
}