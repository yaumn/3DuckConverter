package com.pfa;

import java.util.ArrayList;

/**
 * Superpixel features container
 */
class SuperpixelObject {
    /**
     * Coordinates of the superpixel object
     */
    int iS, iE, jS, jE, iC, jC, jL;

    /**
     * Mean color of the superpixel object
     */
    SuperpixelColor meanColor;
    /**
     * Superpixel object neighbours of the superpixel object
     */
    ArrayList<SuperpixelObject> neighbours;
    /**
     * Large superpixel number associated to the superpixel object
     */
    int lsp;
    /**
     * Disparity of the object
     */
    int objectDisparity;
    /**
     * Number of pixels represnting the object
     */
    int objectMass;
    /**
     * True if the object is considered as ground
     */
    boolean isGround;
    /**
     * True if the object is considered as background
     */
    boolean isSky;

    SuperpixelObject() {
        this.iS = -1;
        this.iE = -1;
        this.jS = -1;
        this.jE = -1;
        this.iC = -1;
        this.jC = -1;
        this.lsp = -1;
        this.objectDisparity = 0;
        this.objectMass = -1;
        this.meanColor = new SuperpixelColor();
        this.neighbours =  new ArrayList<>();
        this.isGround = false;
        this.isSky = false;
    }



}
