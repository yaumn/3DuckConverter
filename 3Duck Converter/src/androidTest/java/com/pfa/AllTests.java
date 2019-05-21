package com.pfa;

import android.Manifest;
import android.os.Environment;
import android.util.Log;

import org.junit.Test;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


/**
 * Execute all tests for the application
 */
public class AllTests {

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("PFA::MainActivity", "Internal OpenCV library not found, exiting.");
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        } else {
            Log.d("PFA::MainActivity", "OpenCV library found inside package. Using it!");
        }
        File rep = new File(Parameters.rootFolder);
        if(rep.list() == null || rep.list().length==0) //si le répertoire n'existait pas ou était vide
            rep.mkdir();
        rep = new File(Parameters.rootFolder + Parameters.testFolder);
        if(rep.list() == null || rep.list().length==0) //si le répertoire n'existait pas ou était vide
            rep.mkdir();
        rep = new File(Parameters.rootFolder + Parameters.testDisparityFolder);
        if(rep.list() == null || rep.list().length==0) //si le répertoire n'existait pas ou était vide
            rep.mkdir();
        rep = new File(Parameters.rootFolder + Parameters.testLeftImageFolder);
        if(rep.list() == null || rep.list().length==0) //si le répertoire n'existait pas ou était vide
            rep.mkdir();
        rep = new File(Parameters.rootFolder + Parameters.testLogFolder);
        if(rep.list() == null || rep.list().length==0) //si le répertoire n'existait pas ou était vide
            rep.mkdir();
    }

    String log = new String();

    /**
     * Execute each test and save a log file
     */
    @Test
    public void PerformAllTests() {
        writeToLog("///Test all disparities///\n");
        writeToLog(DisparityTest.testDisparity());
        writeLog();
    }

    /**
     * Add the sring s at the end of the log
     * @param s the string to be added
     */
    public void writeToLog(String s) {
        log += "\n" + s;
    }


    /**
     * Save the logfile
     */
    public void writeLog() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Parameters.rootFolder + Parameters.testLog,"UTF-8");
            writer.println(log);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
