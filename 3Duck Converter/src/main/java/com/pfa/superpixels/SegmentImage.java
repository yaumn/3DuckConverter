/**
 * Dead code: is not used at all in the project
 */

package com.pfa.superpixels;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;


/**
 * @Deprecated
 */
@Deprecated
public class SegmentImage
{
    static float diff(Mat r, Mat g, Mat b, int x1, int y1, int x2, int y2)
    {
        return (float)Math.sqrt(Math.pow(r.get(x1, y1)[0] - r.get(x2, y2)[0], 2)
            + Math.pow(g.get(x1, y1)[0] - g.get(x2, y2)[0], 2)
            + Math.pow(b.get(x1, y1)[0] - b.get(x2, y2)[0], 2));
    }


    static byte[] randomRgb()
    {
        byte color[] = new byte[3];
        color[0] = (byte)(Math.random() % 256);
        color[1] = (byte)(Math.random() % 256);
        color[2] = (byte)(Math.random() % 256);
        return color;
    }


    static public Mat segment(Mat img, double sigma, double c, int minSize, Integer numCss)
    {
        int width = img.cols();
        int height = img.rows();

        Log.d("pfa::segmentimage", String.format("%d %d %d", width, height, width * height));

        Mat r = new Mat(width, height, CvType.CV_32F);
        Mat g = new Mat(width, height, CvType.CV_32F);
        Mat b = new Mat(width, height, CvType.CV_32F);

        Mat smoothR = Filter.smooth(r, sigma);
        Mat smoothG = Filter.smooth(g, sigma);
        Mat smoothB = Filter.smooth(b, sigma);

        Edge edges[] = new Edge[width * height * 4];
        int num = 0;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                if (x < width - 1) {
                    edges[num] = new Edge();
                    edges[num].a = y * width + x;
                    edges[num].b = y * width + (x + 1);
                    edges[num].w = diff(smoothR, smoothG, smoothB, x, y, x + 1, y);
                    num++;
                }

                if (y < height - 1) {
                    edges[num] = new Edge();
                    edges[num].a = y * width + x;
                    edges[num].b = (y + 1) * width + x;
                    edges[num].w = diff(smoothR, smoothG, smoothB, x, y, x, y + 1);
                    num++;
                }

                if ((x < width - 1) && (y < height - 1)) {
                    edges[num] = new Edge();
                    edges[num].a = y * width + x;
                    edges[num].b = (y + 1) * width + (x + 1);
                    edges[num].w = diff(smoothR, smoothG, smoothB, x, y, x + 1, y + 1);
                    num++;
                }

                if ((x < width - 1) && (y > 0)) {
                    edges[num] = new Edge();
                    edges[num].a = y * width + x;
                    edges[num].b = (y - 1) * width + (x + 1);
                    edges[num].w = diff(smoothR, smoothG, smoothB, x, y, x + 1, y - 1);
                    num++;
                }
            }
        }


        Log.d("pfa::segmentimage", "avant segment");
        Universe u = SegmentGraph.segmentGraph(width * height, num, edges, (float)c);

        for (int i = 0; i < num; i++) {
            int edgeA = u.find(edges[i].a);
            int edgeB = u.find(edges[i].b);
            if ((edgeA != edgeB) && ((u.size(edgeA) < minSize) || (u.size(edgeB) < minSize)))
                u.join(edgeA, edgeB);
        }

        numCss = u.numSets();

        Mat output = new Mat(width, height, img.type());

        byte colors[][] = new byte[width * height][3];
        for (int i = 0 ; i < width * height ; i++) {
            colors[i] = randomRgb();

            Log.d("pfa::segmentimage", String.format("%d %d %d", colors[0], colors[1], colors[2]));
        }

        for (int y = 0 ; y < height - 1 ; y++) {
            for (int x = 0 ; x < width - 1 ; x++) {
                int comp = u.find(y * width + x);
                output.put(x, y, colors[comp]);
            }
        }

        return output;
    }
}
