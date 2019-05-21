package com.pfa;

/**
 * Sub-division of a superpixel and all the transformations associated
 */
public class SuperpixelComponent {

    /**
     * Coordinates of the superpixel component and its center of mass
     */
    int iS, iE, jS, jE, iC, jC;
    /**
     * ids of the superpixels from wich the component is part
     */
    int sp, lsp;
    /**
     * Disparity of the component
     */
    int disparity;

    /**
     * Init a superpixel component
     */
    SuperpixelComponent(int sp, int lsp) {
        this.iS = -1;
        this.iE = -1;
        this.iC = -1;
        this.jS = -1;
        this.jE = -1;
        this.jC = -1;
        this.sp = sp;
        this.lsp = lsp;
        this.disparity = 0;
    }
}
