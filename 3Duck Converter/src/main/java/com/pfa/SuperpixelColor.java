package com.pfa;

import android.util.Log;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Color of a superpixel and all the transformations associated
 */
public class SuperpixelColor {
    /**
     * RGBA values of the mean color of the superpixelObject
     */
    int R, G, B, A;
    /**
     * Threshold to define if two color are considered approximatly equals
     */
    int threshold;

    SuperpixelColor() {
       R = 0;
       G = 0;
       B = 0;
       A = 0;
       threshold = Parameters.sameColorThreshold;
    }

    /**
     * Set the superpixel main color
     * @param color a rgba color
     */
    public void setColor(byte[] color) {
        R = color[0];
        G = color[1];
        B = color[2];
        //A = color[3];
        R += 128;
        G += 128;
        B += 128;
        //A += 128;
    }

    /**
     * Get the superpixel main color
     * @param color  the color in which the superpixel color will be stored
     */
    public void getColor(byte[] color) {
        if (R > 255 || G > 255 || B > 255 || A > 255) {
            Log.d("PFA::COLOR", "Error getting unvalid values");
        }
        color[0] = (byte) (R - 128);
        color[1] = (byte) (G - 128);
        color[2] = (byte) (B - 128);
        //color[3] = (byte) (A - 128);
    }

    /**
     * add the color to the mean superpixel color value
     * @param color the color to be added
     */
    public void addColor(byte[] color) {
        R += color[0];
        G += color[1];
        B += color[2];
        //A += color[3];
        R += 128;
        G += 128;
        B += 128;
        //A += 128;
    }

    /**
     * Divide the mean color (actually at this point its the sum
     * of all the pixels colors from the superpixel) and divide it
     * by divider
     * @param divider the number by wich the mean color will be divised
     */
    public void mean(int divider) {
        R /= divider;
        G /= divider;
        B /= divider;
        A /= divider;
    }

    /**
     * Compare the superpixel mean color with color
     * @param color the color to compare mean color with
     * @return true if color is similar to the superpixel
     * mean color
     */
    public boolean isSimilar(SuperpixelColor color) {
        double colordiff = sqrt(pow(R - color.R, 2)
                + pow(B - color.B, 2)
                + pow(G - color.G, 2)/*
                + pow(A- color.A, 2)*/);
        //Log.d("PFA::COLOR", "RGBA diff : " + colordiff);
        return colordiff <= threshold;
    }
}
