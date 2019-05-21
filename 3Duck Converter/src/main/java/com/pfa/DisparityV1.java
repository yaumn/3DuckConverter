package com.pfa;
/*
 * NOT USED
 */

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ximgproc.SuperpixelSLIC;
import org.opencv.ximgproc.Ximgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.max;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

/**
 * @deprecated replaced by {@link Disparity2}
 */
@Deprecated
public class DisparityV1 {

    byte[] map;
    int[] lowSegmentedMat;
    int[] highSegmentedMat;
    int[] segmentedMat;
    Mat lowSegmentedLabels;
    Mat highSegmentedLabels;
    Mat segmentedLabels;
    Mat heatMap;
    int rows;
    int cols;
    int sky;
    int ground;
    int highSuperpixelNumber;
    int lowSuperpixelNumber;
    int farestPlanDisparity;
    int groundSkySeparation;
    Mat img;
    byte[] testMap;

    //To call when the resolution changed
    public void init(CvCameraViewFrame inputFrame) {
        this.img = inputFrame.rgba();
        Log.d("PFA::DISP", "RGBA type : " + img.type());
        this.rows = img.rows();
        this.cols = img.cols();
        this.map = new byte[rows*cols];
        this.lowSegmentedMat = new int[rows*cols];
        this.highSegmentedMat = new int[rows*cols];
        this.segmentedMat = new int[rows*cols];
        this.lowSegmentedLabels = new Mat();
        this.highSegmentedLabels = new Mat();
        this.segmentedLabels = new Mat();
        this.testMap = new byte[rows*cols];
        this.heatMap = new Mat(rows,cols,CvType.CV_8U);
        this.heatMap.get(0,0, testMap);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void computeDisparity(int farestPlanDisparity) {
        Log.d("PFA::DISP", "Start disparity");

        this.farestPlanDisparity = farestPlanDisparity;
        SuperpixelSLIC lowSup = Ximgproc.createSuperpixelSLIC(img, Ximgproc.MSLIC, 100, 0.075f);
        lowSup.iterate(1);
        lowSup.enforceLabelConnectivity(50);

        SuperpixelSLIC highSup = Ximgproc.createSuperpixelSLIC(img, Ximgproc.MSLIC, 20, 0.075f);
        highSup.iterate(1);
        highSup.enforceLabelConnectivity(50);

        this.highSuperpixelNumber = highSup.getNumberOfSuperpixels();
        Log.d("PFA::DISP", "high number of sup : " + highSuperpixelNumber);
        this.lowSuperpixelNumber = lowSup.getNumberOfSuperpixels();
        Log.d("PFA::DISP", "low number of sup : " + lowSuperpixelNumber);

        lowSup.getLabels(this.lowSegmentedLabels);
        highSup.getLabels(this.highSegmentedLabels);

        getWholeMat(this.lowSegmentedLabels, this.lowSegmentedMat);
        getWholeMat(this.highSegmentedLabels, this.highSegmentedMat);

        /*
        for(int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                map[i+rows*j] = (byte) (highSegmentedMat[i+rows*j] * 255 / highSuperpixelNumber);
            }
        }
        */

        Log.d("PFA::DISP", "Labels type : " + this.highSegmentedLabels.type());
        Log.d("PFA::DISP", "Start finding ground");
        this.findGround();
        Log.d("PFA::DISP", "Found " + ground);

        Log.d("PFA::DISP", "Start finding sky");
        this.findSky();
        Log.d("PFA::DISP", "Found " + sky);
        //Mixing high and low resolution segmentation to treat each object independently
        Log.d("PFA::DISP", "Start recompose");
        this.recompose();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.map[j+cols*i] = (byte) (this.highSegmentedMat[j+cols*i] * 255 / highSuperpixelNumber);
            }
        }

        if(true)
            return;

        ground *= highSuperpixelNumber;
        sky *= highSuperpixelNumber;
        //Give a gradual disparity to the ground from 255 to farestPlanDisparity
        //A disparity of farestPlanDisparity to the sky
        //The relative disparity of each superpixels of each objects + the disparity of the lowest
        //point of the object (considered equal to ground just below or 255 if no ground)
        Log.d("PFA::DISP", "Start disparity inference");
        this.inferDisparity();

        Log.d("PFA::DISP", "End disparity");
    }

    private void findGround() {
        ArrayList<Integer> candidates = new ArrayList<>();
        ArrayList<Integer> masses = new ArrayList<>();
        int m;
        for (int i = rows - rows/10; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (!candidates.contains(lowSegmentedMat[j+cols*i])) {
                    candidates.add(lowSegmentedMat[j+cols*i]);
                    masses.add(1);
                } else {
                    m = candidates.indexOf(lowSegmentedMat[j+cols*i]);
                    masses.set(m, masses.get(m) + 1);
                }
            }
        }

        ground = candidates.get(0);
        int groundMass = masses.get(0);
        Log.d("PFA::DISP","Ground candidate " + candidates.get(0) + " of mass " + masses.get(0));
        for (int i = 1; i < candidates.size(); i++) {
            Log.d("PFA::DISP","Ground candidate " + candidates.get(i) + " of mass " + masses.get(i));
            if (masses.get(i) > groundMass) {
                ground = candidates.get(i);
                groundMass = masses.get(i);
            }
        }
        Log.d("PFA::DISP", "Ground chosen : " + ground);
    }

    private void findSky() {

        List<Integer> candidates = new ArrayList<>();
        List <Integer> masses = new ArrayList<>();
        int m;
        for (int i = 0; i < rows/10; i++) {
            for (int j = 0; j < cols; j++) {
                if (!candidates.contains(this.lowSegmentedMat[j+i*cols])) {
                    candidates.add(this.lowSegmentedMat[j+i*cols]);
                    masses.add(1);
                } else {
                    m = candidates.indexOf(this.lowSegmentedMat[j+i*cols]);
                    masses.set(m, masses.get(m)+1);
                }
            }
        }

        sky = candidates.get(0);
        int skyMass = masses.get(0);
        Log.d("PFA::DISP","Sky candidate " + candidates.get(0) + " of mass " + masses.get(0));
        for (int i = 1; i < candidates.size(); i++) {
            Log.d("PFA::DISP","Sky candidate " + candidates.get(i) + " of mass " + masses.get(i));
            if (masses.get(i) > skyMass) {
                sky = candidates.get(i);
                skyMass = masses.get(i);
            }
        }
        Log.d("PFA::DISP", "Sky chosen : " + sky);
    }

    //Merge low segmentation for sky and ground with high segmentation for other objects.
    private void recompose() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (this.sky == lowSegmentedMat[j+i*cols] || this.ground == lowSegmentedMat[j+i*cols]) {
                    this.segmentedMat[j+i*cols] = this.lowSegmentedMat[j+i*cols] * highSuperpixelNumber;
                } else {
                    segmentedMat[j+i*cols] = highSegmentedMat[j+i*cols]
                            + lowSegmentedMat[j+i*cols] * highSuperpixelNumber;
                }
            }
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    private void inferDisparity() {
        Log.d("PFA::Disparity::infer", "InferDisparity Start");
        //setup ground disparity
        this.groundSkySeparation = this.separateGroundSky();
        Log.d("PFA::Disparity::infer", "Ground and sky separated : " + groundSkySeparation);

        for (int i = this.groundSkySeparation; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.groundDisparity(i, j);
            }
        }
        Log.d("PFA::Disparity::infer", "Ground disparity set : " + ground);

        //setup sky disparity
        for (int i = 0; i < this.groundSkySeparation; i++) {
            for (int j = 0; j < cols; j++) {
                this.skyDisparity(i, j);
            }
        }
        Log.d("PFA::Disparity::infer", "Sky disparity set : " + sky);

        if(true)
            return;
        //setup object disparity
        for (int lsp = 0; lsp < this.lowSuperpixelNumber; lsp++) {
            if (lsp != ground/highSuperpixelNumber && lsp != sky/highSuperpixelNumber) {
                Log.d("PFA::DISP", "Computing inner lsp : " + lsp);
                SuperpixelObject superpixelObject = new SuperpixelObject();
                superpixelObject.lsp = lsp;
                this.highestGroundDisparityUnderObject(superpixelObject);

                //Infer disparity to the whole object based on masses of each superpixel
                this.innerDisparity(superpixelObject);
            }
        }
    }

    //set disparity map of the ground from 255 to farestPlanDisparity (0 to objects)
    private void groundDisparity(int i, int j) {
        this.map[j+i*cols] = (byte) (farestPlanDisparity + (255-farestPlanDisparity) * (i - groundSkySeparation) / (rows - groundSkySeparation));
    }

    //set disparity map of the sky to farestPlanDisparity (0 to objects)
    private void skyDisparity(int i, int j) {
        this.map[j+i*cols] = (byte) farestPlanDisparity;
    }

    // For the moment gives the separation line between sky and ground;
    private int separateGroundSky() {
        Log.d("PFA::DISP", "cols/10 : " + cols/10+ " sky : " + sky);
        boolean stillSky = true;
        int countSky;
        int i = 0;
        while(stillSky) {
            countSky = 0;
            for (int j = 0; j < cols; j++) {
                if (segmentedMat[j+cols*i] == sky) {
                    countSky++;
                }
            }
            i++;
            //threshold for sky may have to change
            if(i >= rows || countSky < cols/10) {
                stillSky = false;
            }
        }
        return i;
    }


    //Compute disparity of the lowest point of the object
    //It also computes the coordinates of the object to lower the cost
    private void highestGroundDisparityUnderObject(SuperpixelObject superpixelObject) {
        int jLeftest = -1, jRightest = -1, iHighest = -1, iLowest = -1, jLast = -1;
        for (int i = 0; i < segmentedLabels.rows(); i++) {
            for (int j = 0; j < segmentedLabels.cols(); j++) {
                //if the pixel is in the object
                if (segmentedMat[i+j*rows] >= superpixelObject.lsp*highSuperpixelNumber &&
                        this.segmentedMat[i+j*rows] < (superpixelObject.lsp+1)*highSuperpixelNumber) {
                    if (jLeftest == -1 || j < jLeftest) {
                        jLeftest = j;
                    }
                    if (iHighest == -1) {
                        iHighest = i;
                    }
                    if (jRightest < j) {
                        jRightest = j;
                    }
                    iLowest = i;
                    jLast = j;
                }
            }
        }
        superpixelObject.iS = iHighest;
        superpixelObject.iE = iLowest;
        superpixelObject.jS = jLeftest;
        superpixelObject.jE = jRightest;


        Log.d("PFA::DISP", "iS : " + superpixelObject.iS
                + " iE : " +superpixelObject.iE + " jS : "+ superpixelObject.jS
                + " jE : " + superpixelObject.jE + " jLast : " + jLast);

        for (int i = iLowest; i < rows; i++) {

            if (this.segmentedMat[i+jLast*rows] == ground) {
                superpixelObject.objectDisparity = this.map[i+jLast*rows];
                return;
            }
        }
        superpixelObject.objectDisparity = 255;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    //Compute disparity of each part of each object (growing from outside in with outside equal
    //to ground disparity under the object)
    private void innerDisparity(SuperpixelObject superpixelObject) {
        Log.d("PFA::Disparity::infer", "Inner disparity of : " + superpixelObject.lsp);
        this.computeObjectCenter(superpixelObject);
        double objectRadius = (sqrt((superpixelObject.iC - superpixelObject.iS) ^ 2
                + (superpixelObject.jC - superpixelObject.jS) ^ 2)
                + sqrt((superpixelObject.iC - superpixelObject.iS) ^ 2 +
                (superpixelObject.jC - superpixelObject.jS) ^ 2)) / 2;
        int objectCenterDisparity = 255 * superpixelObject.objectMass/(rows*cols);
        for (int i = superpixelObject.iS; i < superpixelObject.iE; i++) {
            for (int j = superpixelObject.jS; j < superpixelObject.jE; j++) {
                if (this.map[i+j*rows] == -1) {
                    SuperpixelComponent superpixelComponent = new SuperpixelComponent(0,0);
                    this.computeComponentCenter(i,superpixelObject.iE, superpixelObject.jS, superpixelObject.jE, superpixelComponent, segmentedMat[i+j*rows]);
                    for (int iComponent = superpixelObject.iS; iComponent < superpixelObject.iE; iComponent++) {
                        for (int jComponent = superpixelObject.jS; jComponent < superpixelObject.jE; jComponent++) {
                            this.map[i+rows*j] = (byte) (superpixelObject.objectDisparity
                                    + objectCenterDisparity * (int) round(max((double) 0,
                                    floor(1 - sqrt((superpixelObject.iC - superpixelComponent.iC)
                                            + (superpixelObject.jC - superpixelComponent.jC))/objectRadius))));
                        }
                    }
                }
            }
        }
    }

    //Set object characteristics
    private void computeObjectCenter(SuperpixelObject superpixelObject) {
        int iSum = 0;
        int jSum = 0;
        for (int i = superpixelObject.iS; i < superpixelObject.iE; i++) {
            for (int j = superpixelObject.jS; j < superpixelObject.jE; j++) {
                if (segmentedMat[i+rows*j] >= highSuperpixelNumber * superpixelObject.lsp
                        && segmentedMat[i+rows*j] < highSuperpixelNumber *(superpixelObject.lsp+1)) {
                    iSum += i;
                    jSum += j;
                    superpixelObject.objectMass += 1;
                }
            }
        }
        superpixelObject.iC = iSum / superpixelObject.objectMass;
        superpixelObject.jC = jSum / superpixelObject.objectMass;
    }

    //Set component characteristicsObjectCenter
    private void computeComponentCenter(int iS, int iE, int jS, int jE, SuperpixelComponent superpixelComponent, int sp) {
        int iSum = 0, jSum = 0, componentMass = 0;
        for (int i = iS; i < iE; i++) {
            for (int j = jS; j < jE; j++) {
                if (segmentedMat[i+rows*j] == sp) {
                    iSum += i;
                    jSum += j;
                    componentMass += 1;
                }
            }
        }
        superpixelComponent.iC = iSum / componentMass;
        superpixelComponent.jC = jSum / componentMass;
    }

    void getWholeMat(Mat mat, int[] buffer){
        if (mat.isContinuous()) {
            mat.get(0,0, buffer);
        } else {
            Log.d("PFA::DISPARITY", "ERROR MAT NOT CONTINUOUS");
        }
    }

    void setWholeMat(Mat mat, int[] buffer) {
        if (mat.isContinuous()) {
            mat.put(0,0, buffer);
        } else {
            Log.d("PFA::DISPARITY", "ERROR MAT NOT CONTINUOUS");
        }
    }
    //*/
}
