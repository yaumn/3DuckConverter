package com.pfa;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ximgproc.SuperpixelSLIC;
import org.opencv.ximgproc.Ximgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;

/**
 * Compute disparity from an image or frame
 */
class Disparity2 {
    /**
     * Array containing the final disparities of each pixels
     */
    private byte[] map;
    /**
     * Array containing the corresponding large superpixels ids of each pixels
     */
    private int[] lowSegmentedMat;
    /**
     * Array containing the corresponding small superpixels ids of each pixels
     */
    private int[] highSegmentedMat;
    /**
     * Array containing the corresponding superpixels ids of each pixels
     */
    private int[] segmentedMat;
    /**
     * OpenCV Mat version of lowSegmentedMat
     */
    private Mat lowSegmentedLabels;
    /**
     * OpenCV Mat version of highSegmentedMat
     */
    private Mat highSegmentedLabels;
    /**
     * OpenCV Mat version of map
     */
    Mat heatMap;
    /**
     * Number of rows of the input image (after resizing)
     */
    private int rows;
    /**
     * Number of cols of the input image (after resizing)
     */
    private int cols;
    /**
     * id of the best background candidate of all the superpixel
     */
    private int sky;
    /**
     * id of the best ground candidate of all the superpixel
     */
    private int ground;
    /**
     * Number of small superpixels
     */
    private int highSuperpixelNumber;
    /**
     * Number of large superpixels
     */
    private int lowSuperpixelNumber;
    /**
     * Minimum disparity, affected to the background only
     */
    private int farestPlanDisparity;
    /**
     * Line at which the background end and the ground start
     */
    private int groundSkySeparation;
    /**
     * Mat containing the input frame
     */
    private Mat img;
    /**
     * Array containg the input frame decomposed by channel
     */
    private byte[][] imgMat;
    /**
     * List of all superpixel objects found
     */
    private SuperpixelObject[] superpixelObjects;

    /**
     * Real size of the input image
     */
    private Size sz;
    /**
     * True if the image wasn't scaled down during conversion
     */
    private boolean oriSize;

    /**
     * Abstract representation of the number of large superpixels desired
     */
    private int largeSupNumber = Parameters.maxLargeSuperpixelNumber;
    /**
     * Abstract representation of the number of small superpixels desired
     */
    private int narrowSupNumber = Parameters.maxSmallSuperpixelNumber;
    /**
     * Region size for the large segmentation
     */
    private int largeSupSize;
    /**
     * Region size for the narrow segmentation
     */
    private int narrowSupSize;
    /**
     * Define the maximum number of row to compute a disparity map
     * if realTime is true.
     * If the input image is bigger, a resized image will be used then
     * the disparity map will be resized to fit the input image.
     */
    private int MAX_ROW_SIZE = Parameters.maxHeatmapRow;

    /**
     * Require that img is initialized by calling init(img)
     * Initialise all the parameters to compute the disparity
     */
    private void init() {
        this.rows = img.rows();
        this.cols = img.cols();
        this.map = new byte[rows*cols];
        this.lowSegmentedMat = new int[rows*cols];
        this.highSegmentedMat = new int[rows*cols];
        this.segmentedMat = new int[rows*cols];
        this.lowSegmentedLabels = new Mat();
        this.highSegmentedLabels = new Mat();
        this.heatMap = new Mat(rows,cols, CvType.CV_8U);
        this.imgMat = getMultiChannelArray(img);
        this.largeSupSize = rows / this.largeSupNumber;
        this.narrowSupSize = rows / this.narrowSupNumber;
        Log.d("PFA::DISP", "Sup Size : " + largeSupSize + " " + narrowSupSize);
    }

    /**
     * Initialize the disparity with the rgba contained in the input frame
     * @param inputFrame  The frame used for initialization
     */
    void init(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        init(inputFrame.rgba());
    }

    /**
     * Initialize the disparity with inputFrame
     * @param inputFrame the image from which the disparity will be computed
     */
    void init(Mat inputFrame) {
        img = new Mat(inputFrame.size(), inputFrame.type());
        oriSize = true;
        if (Parameters.realTime && inputFrame.rows() > MAX_ROW_SIZE) {
            sz = inputFrame.size();
            Size szTemp = new Size(MAX_ROW_SIZE, MAX_ROW_SIZE*4/5);
            Imgproc.resize(inputFrame, img, szTemp, 0,0, INTER_AREA);
            oriSize = false;
        } else {
            img = inputFrame;
        }
        System.out.format("pfa::disp imsize : %dx%d, type %d\n", img.rows(), img.cols(), img.type());
        Log.d("PFA::DISP", "RGBA type : " + img.type());
        this.init();
    }



    /**
     * The full computation of the disparity map of img
     * @param farestPlanDisparity The disparity will range from farestPlanDisparity to 255
     */
    void computeDisparity(int farestPlanDisparity) {
        Log.d("PFA::DISP", "Start disparity");

        this.farestPlanDisparity = farestPlanDisparity;
        SuperpixelSLIC lowSup = Ximgproc.createSuperpixelSLIC(img, Ximgproc.MSLIC, largeSupSize, 0.075f);
        lowSup.iterate(1);
        lowSup.enforceLabelConnectivity(50);

        SuperpixelSLIC highSup = Ximgproc.createSuperpixelSLIC(img, Ximgproc.MSLIC, narrowSupSize, 0.025f);
        highSup.iterate(1);
        highSup.enforceLabelConnectivity(50);

        this.highSuperpixelNumber = highSup.getNumberOfSuperpixels();
        Log.d("PFA::DISP", "high number of sup : " + highSuperpixelNumber);
        this.lowSuperpixelNumber = lowSup.getNumberOfSuperpixels();
        Log.d("PFA::DISP", "low number of sup : " + lowSuperpixelNumber);

        lowSup.getLabels(this.lowSegmentedLabels);
        highSup.getLabels(this.highSegmentedLabels);

        this.getWholeMat(this.lowSegmentedLabels, this.lowSegmentedMat);
        this.getWholeMat(this.highSegmentedLabels, this.highSegmentedMat);

        this.mapSuperpixels();

        Log.d("PFA::DISP", "Labels type : " + this.highSegmentedLabels.type());
        Log.d("PFA::DISP", "Start finding ground");
        this.findBestGroundCandidate();
        Log.d("PFA::DISP", "Found " + ground);
        Log.d("PFA::DISP", "Start finding sky");
        this.findBestSkyCandidate();
        Log.d("PFA::DISP", "Found " + sky);

        this.computeGround2();
        this.computeSky2();

        //Mixing high and low resolution segmentation to treat each object independently
        Log.d("PFA::DISP", "Start recompose");
        this.recompose();

        //Give a gradual disparity to the ground from 255 to farestPlanDisparity
        //A disparity of farestPlanDisparity to the sky
        //The relative disparity of each superpixels of each objects + the disparity of the lowest
        //point of the object (considered equal to ground just below or 255 if no ground)
        Log.d("PFA::DISP", "Start disparity inference");
        this.inferDisparity();


        heatMap.put(0, 0, map);

        System.out.format("pfa::disp Heatmap before %dx%d, channel %d, type %d\n", heatMap.rows(), heatMap.cols(), heatMap.channels(), heatMap.type());

        if (!oriSize) {
            System.out.format("pfa::disp Resizing\n");
            Mat dst = new Mat(heatMap.size(), heatMap.type());
            Imgproc.resize(heatMap, dst, sz, 0, 0, INTER_LINEAR);
            heatMap = dst;
        }

        if (Parameters.saveDisparity) {
            ImageConversion.saveDisparity(heatMap, "heatmap_disparity.jpg");
        }

        System.out.format("pfa::disp Heatmap %dx%d, channel %d, type %d\n", heatMap.rows(), heatMap.cols(), heatMap.channels(), heatMap.type());
        Log.d("PFA::DISP", "End disparity");
    }

    /**
     * Isolate each superpixel from the low segmentation and
     * compute some of its features like neighbours, coordinates,
     * mass in pixel, center of mass, average color
     */
    private void mapSuperpixels() {

        this.superpixelObjects = new SuperpixelObject[lowSuperpixelNumber];

        for (int i = 0; i < lowSuperpixelNumber; i++) {
            superpixelObjects[i] = new SuperpixelObject();
            superpixelObjects[i].lsp = i;
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                int lsp = lowSegmentedMat[j+i*cols];

                //Init neighborhood graph

                if (i < rows -1) {
                    int next = lowSegmentedMat[j + (i + 1) * cols];
                    if (lsp != next) {
                        superpixelObjects[lsp].neighbours.add(superpixelObjects[next]);
                        superpixelObjects[next].neighbours.add(superpixelObjects[lsp]);
                    }
                }
                if (j < cols -1) {
                    int next = lowSegmentedMat[j + 1 + i * cols];
                    if (lsp != next) {
                        superpixelObjects[lsp].neighbours.add(superpixelObjects[next]);
                        superpixelObjects[next].neighbours.add(superpixelObjects[lsp]);
                    }
                }

                //Map each superpixel
                if (superpixelObjects[lsp].iS == -1 || superpixelObjects[lsp].iS > i) {
                    superpixelObjects[lsp].iS = i;
                }
                if (superpixelObjects[lsp].jS == -1 || superpixelObjects[lsp].jS > j) {
                    superpixelObjects[lsp].jS = j;
                }
                if (superpixelObjects[lsp].iE == -1 || superpixelObjects[lsp].iE < i) {
                    superpixelObjects[lsp].iE = i;
                    superpixelObjects[lsp].jL = j;
                }
                if (superpixelObjects[lsp].jE == -1 || superpixelObjects[lsp].jE < j) {
                    superpixelObjects[lsp].jE = j;
                }

                //Compute each superpixel features
                superpixelObjects[lsp].objectMass += 1;
                superpixelObjects[lsp].iC += i;
                superpixelObjects[lsp].jC += j;
                superpixelObjects[lsp].meanColor.addColor(imgMat[j+i*cols]);
            }
        }

        //End each superpixel features computation
        for (int lsp = 0; lsp < lowSuperpixelNumber; lsp++) {
            superpixelObjects[lsp].iC /= superpixelObjects[lsp].objectMass;
            superpixelObjects[lsp].jC /= superpixelObjects[lsp].objectMass;
            superpixelObjects[lsp].meanColor.mean(superpixelObjects[lsp].objectMass);
            superpixelObjects[lsp].iE += 1;
            superpixelObjects[lsp].jE += 1;
        }
    }

    //Merge low segmentation for sky and ground with high segmentation for other objects.
    private void recompose() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int lsp = lowSegmentedMat[j+i*cols];
                if (superpixelObjects[lsp].isGround) {
                    this.segmentedMat[j+i*cols] = ground;
                } else if (superpixelObjects[lsp].isSky) {
                    this.segmentedMat[j+i*cols] = sky;
                } else {
                    segmentedMat[j+i*cols] = highSegmentedMat[j+i*cols]
                            + lowSegmentedMat[j+i*cols] * highSuperpixelNumber;
                }
            }
        }
    }

    /**
     * Check if mat is continuous and if yes retrieve its data in buffer
     * @param mat   the matrix from which the data are retrieved
     * @param buffer    the buffer which will store the data
     */
    private void getWholeMat(Mat mat, int[] buffer){
        if (mat.isContinuous()) {
            mat.get(0,0, buffer);
        } else {
            Log.d("PFA::DISPARITY", "ERROR MAT NOT CONTINUOUS");
        }
    }

    /**
     * Check if mat is continuous and if yes store in it the content of buffer
     * @param mat the matrix to fill
     * @param buffer the data to store in mat
     */
    private void setWholeMat(Mat mat, int[] buffer) {
        if (mat.isContinuous()) {
            mat.put(0, 0, buffer);
        } else {
            Log.d("PFA::DISPARITY", "ERROR MAT NOT CONTINUOUS");
        }
    }

    /**
     * Store in ground the id of the superpixel which have the highest
     * chance to be a part of the ground plan
     */
    private void findBestGroundCandidate() {
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

    /**
     * Store in sky the id of the superpixel which have the most
     * chance to be a part of the background plan.
     */
    private void findBestSkyCandidate() {

        List<Integer> candidates = new ArrayList<>();
        List<Integer> masses = new ArrayList<>();
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


    /**
     * Asses which superpixels are most likely part of the ground plan
     */
    private void computeGround2() {
        //ArrayList<SuperpixelObject> grounds = new ArrayList<>();
        ArrayList<SuperpixelObject> groundsCandidates = new ArrayList<>();

        superpixelObjects[ground].isGround = true;
        for (int lsp = 0; lsp < lowSuperpixelNumber; lsp++) {
            if (superpixelObjects[lsp].meanColor.isSimilar(superpixelObjects[ground].meanColor)) {
                superpixelObjects[lsp].isGround = true;
                //grounds.add(superpixelObjects[lsp]);
                groundsCandidates.addAll(superpixelObjects[lsp].neighbours);
            }
        }


        while (groundsCandidates.size() != 0) {
            SuperpixelObject candidate = groundsCandidates.get(0);
            if (!candidate.isGround) {
                for (int i = 0; i < candidate.neighbours.size(); i++) {
                    if (candidate.neighbours.get(i).isGround
                            && candidate.meanColor.isSimilar(candidate.neighbours.get(i).meanColor)) {
                        candidate.isGround = true;
                        //grounds.add(candidate);
                        groundsCandidates.addAll(candidate.neighbours);
                        break;
                    }
                }
            }
            groundsCandidates.remove(candidate);
        }
    }


    /**
     * Old version of computeGround2
     */
    private void computeGround() {
        superpixelObjects[ground].isGround = true;
        for (int lsp = 0; lsp < lowSuperpixelNumber; lsp++) {
            if (superpixelObjects[lsp].meanColor.isSimilar(superpixelObjects[ground].meanColor)) {
                superpixelObjects[lsp].isGround = true;
            }
        }
        ground *= highSuperpixelNumber;
    }

    /**
     * Asses which superpixels are most likely part of
     * the background plan
     */
    private void computeSky2() {
        //ArrayList<SuperpixelObject> skys = new ArrayList<>();
        ArrayList<SuperpixelObject> skysCandidates = new ArrayList<>();

        superpixelObjects[sky].isSky = true;
        for (int lsp = 0; lsp < lowSuperpixelNumber; lsp++) {
            if (superpixelObjects[lsp].meanColor.isSimilar(superpixelObjects[sky].meanColor)) {
                superpixelObjects[lsp].isSky = true;
                //skys.add(superpixelObjects[lsp]);
                skysCandidates.addAll(superpixelObjects[lsp].neighbours);
            }
        }


        while (skysCandidates.size() != 0) {
            SuperpixelObject candidate = skysCandidates.get(0);
            if (!candidate.isSky) {
                for (int i = 0; i < candidate.neighbours.size(); i++) {
                    if (candidate.neighbours.get(i).isSky
                            && candidate.meanColor.isSimilar(candidate.neighbours.get(i).meanColor)) {
                        candidate.isSky = true;
                        //skys.add(candidate);
                        skysCandidates.addAll(candidate.neighbours);
                        break;
                    }
                }
            }
            skysCandidates.remove(candidate);
        }
    }

    /**
     * Old version of computeSky2
     */
    private void computeSky() {
        superpixelObjects[sky].isSky = true;
        for (int lsp = 0; lsp < lowSuperpixelNumber; lsp++) {
            if (superpixelObjects[lsp].meanColor.isSimilar(superpixelObjects[sky].meanColor)) {
                superpixelObjects[lsp].isSky = true;
            }
        }
        sky *= highSuperpixelNumber;
    }


    /**
     * Retrieve each channel of a Matrix in list of vectors
     * @param m the multi-channel array to be split by channel
     * @return a list indexed by the number of channel
     * and containing a vector with the data of the corresponding
     * channel
     */
    private static byte[][] getMultiChannelArray(Mat m) {
        //first index is pixel, second index is channel
        int numChannels=m.channels();//is 3 for 8UC3 (e.g. RGB)
        int frameSize=m.rows()*m.cols();
        byte[] byteBuffer= new byte[frameSize*numChannels];
        m.get(0,0,byteBuffer);

        //write to separate R,G,B arrays
        byte[][] out=new byte[frameSize][numChannels];
        for (int p=0,i = 0; p < frameSize; p++) {
            for (int n = 0; n < numChannels; n++,i++) {
                out[p][n]=byteBuffer[i];
            }
        }
        return out;
    }

    /**
     * Transpose all calculated features into a disparity map
     */
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

        //setup object disparity
        for (int lsp = 0; lsp < this.lowSuperpixelNumber; lsp++) {
            if (!superpixelObjects[lsp].isGround && !superpixelObjects[lsp].isSky) {
                //Log.d("PFA::DISP", "Computing inner lsp : " + lsp);
                this.highestGroundDisparityUnderObject(superpixelObjects[lsp]);

                //Infer disparity to the whole object based on masses of each superpixel
                this.innerDisparity(superpixelObjects[lsp]);
            }
        }
    }


    /**
     * Set the disparity of the pixels considered as ground
     * @param i abscissa of the pixel
     * @param j ordinate of the pixel
     */
    private void groundDisparity(int i, int j) {
        if (this.segmentedMat[j+i*cols] == ground || this.segmentedMat[j+i*cols] == sky) {
            this.map[j+i*cols] = (byte) (farestPlanDisparity + (255-farestPlanDisparity) * (i - groundSkySeparation) / (rows - groundSkySeparation));
        } else {
            this.map[j+i*cols] = 0;
        }
    }

    /**
     * Set the disparity of the pixels considered as part of the background
     * @param i abscissa of the pixel
     * @param j ordinate of the pixel
     */
    private void skyDisparity(int i, int j) {
        if (this.segmentedMat[j+i*cols] == sky || this.segmentedMat[j+i*cols] == ground) {
            this.map[j+i*cols] = (byte) farestPlanDisparity;
        } else {
            this.map[j+i*cols] = 0;
        }
    }

    /**
     * Give the number of the line separating the sky and the ground
     * @return the line number
     */
    private int separateGroundSky() {
        Log.d("PFA::DISP", "cols/10 : " + cols/Parameters.groundSkyFindLimit+ " sky : " + sky);
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

    /**
     * Compute disparity of the lowest point of the object
     */
    private void highestGroundDisparityUnderObject(SuperpixelObject superpixelObject) {
        //Log.d("PFA::DISP", "iS : " + superpixelObject.iS
        //        + " iE : " +superpixelObject.iE + " jS : "+ superpixelObject.jS
        //        + " jE : " + superpixelObject.jE + " jC : " + superpixelObject.jC + " jL : " + superpixelObject.jL);

        for (int i = superpixelObject.iE; i < rows; i++) {
            if (superpixelObjects[this.lowSegmentedMat[superpixelObject.jL + i*cols]].isGround) {
                superpixelObject.objectDisparity = this.map[superpixelObject.jL + i*cols];
                //Log.d("PFA::DISP", "Disparity of : " + superpixelObject.objectDisparity);
                break;
            }
        }

        if (superpixelObject.objectDisparity == 0) {
            superpixelObject.objectDisparity = 255;
        }
    }

    /**
     * Old version of innerDisparity
     */
    private void innerDisparityOld(SuperpixelObject superpixelObject) {
        for (int i = superpixelObject.iS; i < superpixelObject.iE; i++) {
            for (int j = superpixelObject.jS; j < superpixelObject.jE; j++) {
                if (this.segmentedMat[j+i*cols] >= superpixelObject.lsp * highSuperpixelNumber && this.segmentedMat[j+i*cols] < (superpixelObject.lsp+1) * highSuperpixelNumber && this.map[j+i*cols] == 0) {
                    this.map[j+i*cols] = (byte) superpixelObject.objectDisparity;
                }
            }
        }
    }

    /**
     * Compute disparity of each part of each object (growing from outside in, with outside equal
     * to ground disparity under the object)
     */
    private void innerDisparity(SuperpixelObject superpixelObject) {
        //Log.d("PFA::Disparity::infer", "Inner disparity of superpixel : " + superpixelObject.lsp);
        double objectRadius = (sqrt((superpixelObject.iC - superpixelObject.iS) ^ 2
                + (superpixelObject.jC - superpixelObject.jS) ^ 2)
                + sqrt((superpixelObject.iC - superpixelObject.iS) ^ 2 +
                (superpixelObject.jC - superpixelObject.jS) ^ 2)) / 2;
        int objectCenterDisparity = 255 * superpixelObject.objectMass/(rows*cols);
        for (int i = superpixelObject.iS; i < superpixelObject.iE; i++) {
            for (int j = superpixelObject.jS; j < superpixelObject.jE; j++) {
                if (this.segmentedMat[j+i*cols] >= superpixelObject.lsp * highSuperpixelNumber && this.segmentedMat[j+i*cols] < (superpixelObject.lsp+1) * highSuperpixelNumber && this.map[j+i*cols] == 0) {
                    SuperpixelComponent superpixelComponent = new SuperpixelComponent(this.segmentedMat[j+i*cols], superpixelObject.lsp);
                    this.computeComponentCenter(i,superpixelObject.iE, superpixelObject.jS, superpixelObject.jE, superpixelComponent);
                    superpixelComponent.disparity = superpixelObject.objectDisparity
                            + objectCenterDisparity * (int) round(max((double) 0,
                            floor(1 - sqrt((superpixelObject.iC - superpixelComponent.iC)^2
                                    + (superpixelObject.jC - superpixelComponent.jC)^2) / objectRadius)));
                    //Log.d("PFA::DISP", "Component disparity added = " + (superpixelComponent.disparity - superpixelObject.objectDisparity) + ", radius : " + objectRadius + ", dist : " + (1 - sqrt((superpixelObject.iC - superpixelComponent.iC)^2 + (superpixelObject.jC - superpixelComponent.jC)^2)/objectRadius));
                    for (int iComponent = superpixelComponent.iS; iComponent < superpixelComponent.iE; iComponent++) {
                        for (int jComponent = superpixelComponent.jS; jComponent < superpixelComponent.jE; jComponent++) {
                            if (superpixelComponent.sp == this.segmentedMat[jComponent+iComponent*cols]) {
                                this.map[jComponent+cols*iComponent] = (byte) superpixelComponent.disparity;//*/

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set component characteristics
     */
    private void computeComponentCenter(int iS, int iE, int jS, int jE, SuperpixelComponent superpixelComponent) {
        int iSum = 0, jSum = 0, componentMass = 0;
        for (int i = iS; i < iE; i++) {
            for (int j = jS; j < jE; j++) {

                if (segmentedMat[j+i*cols] == superpixelComponent.sp) {

                    if (superpixelComponent.iS == -1 || superpixelComponent.iS >= i) {
                        superpixelComponent.iS = i;
                    }
                    if (superpixelComponent.jS == -1 || superpixelComponent.jS >= j) {
                        superpixelComponent.jS = j;
                    }
                    if (superpixelComponent.iE == -1 || superpixelComponent.iE <= i) {
                        superpixelComponent.iE = i;
                    }
                    if (superpixelComponent.jE == -1 || superpixelComponent.jE <= j) {
                        superpixelComponent.jE = j;
                    }

                    iSum += i;
                    jSum += j;
                    componentMass += 1;
                }
            }
        }
        if (componentMass == 0) {
            Log.d("PFA::ERROR", "Component Mass = 0, sp : " + superpixelComponent.sp + ", iS, iE, jS, jE : " + iS + " " + iE + " " + jS + " " + jE);
        }
        superpixelComponent.iC = iSum / componentMass;
        superpixelComponent.jC = jSum / componentMass;
        superpixelComponent.iE += 1;
        superpixelComponent.jE += 1;
    }

    /** Smoothen the disparity that might change too much
     * from frame to frame.
     * The idea is to look around the pixel in a 20x20 square
     * to find the previous max value,
     * if it changed more than that the disparity need smoothing
     * (otherwise an object might pop  from front to back each frame
     * @param oldDisp is the last frame's disparity map
     * @param newDisp is the current disparity map of the frame being computed
     * and will be directly modified through this function
     * @param min is the minimum disparity value
     * @param max is the maximum disparity value
     *
     */
    private void smoothDisparity(Mat oldDisp, Mat newDisp, double min, double max){
        // 3 arbitrary and tweakable values
        int squareRadius = 10;
        double smoothFactor = 0.1;
        double threshold = (max-min)/5;

        double limitDisp;
        rows = newDisp.rows();
        cols = newDisp.cols();

        // For each pixel
        for(int i_p = 0; i_p < rows; i_p++) {
            for(int j_p = 0; j_p < cols; j_p++) {

                int beginX, endX, beginY, endY;
                beginX = (i_p<squareRadius-1 ? 0 : i_p-squareRadius);
                beginY = (j_p<squareRadius-1 ? 0 : j_p-squareRadius);
                endX = (newDisp.cols() < i_p+squareRadius ? newDisp.cols() : i_p+squareRadius);
                endY = (newDisp.rows() < i_p+squareRadius ? newDisp.rows() : i_p+squareRadius);

                if(threshold < abs(newDisp.get(i_p,j_p)[0] - oldDisp.get(i_p,j_p)[0])){
                    if (newDisp.get(i_p,j_p)[0] < oldDisp.get(i_p,j_p)[0]){
                        // It dropped too quick ----------------------------------------------------
                        limitDisp = max;
                        // Searching the limitDisp in the previous 20x20 disp around the pixel
                        for(int i = beginX ; i < endX ; i++) {
                            for(int j = beginY ; j < endY ; j++) {
                                if (oldDisp.get(i_p,j_p)[0] < limitDisp){
                                    limitDisp = oldDisp.get(i_p,j_p)[0];
                                }
                            }
                        }
                        if(newDisp.get(i_p,j_p)[0] <= limitDisp){
                            newDisp.get(i_p,j_p)[0] = oldDisp.get(i_p,j_p)[0] - smoothFactor*abs(newDisp.get(i_p,j_p)[0] - oldDisp.get(i_p,j_p)[0]);
                        }
                    } else {
                        // It Augmented too quick --------------------------------------------------
                        limitDisp = min;
                        // Searching the limitDisp in the previous 20x20 disp around the pixel
                        for(int i = beginX ; i < endX ; i++) {
                            for(int j = beginY ; j < endY ; j++) {
                                if (oldDisp.get(i_p,j_p)[0] > limitDisp){
                                    limitDisp = oldDisp.get(i_p,j_p)[0];
                                }
                            }
                        }
                        if(newDisp.get(i_p,j_p)[0] >= limitDisp){
                            newDisp.get(i_p,j_p)[0] = oldDisp.get(i_p,j_p)[0] + smoothFactor*abs(newDisp.get(i_p,j_p)[0] - oldDisp.get(i_p,j_p)[0]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Give the maximum value of (a,b)
     */
    private double max(double a, double b) {
        if (a > b)
            return a;
        return b;
    }
}
