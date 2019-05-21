package com.pfa;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.CallSuper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle the whole activity of the application
 */
public class MainActivity extends Activity
{
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    static boolean openCVLoaded = false;



    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("PFA::MainActivity", "Internal OpenCV library not found, exiting.");
        } else {
            Log.d("PFA::MainActivity", "OpenCV library found inside package. Using it!");
            openCVLoaded = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        CameraManager manager = getSystemService(CameraManager.class);
        try {
            List<String> spinnerArray =  new ArrayList<String>();

            // Different modes
            spinnerArray.add("Normal");
            spinnerArray.add("Simulate");
            spinnerArray.add("Correct");
            spinnerArray.add("Segmentation");
            spinnerArray.add("Heat Map");
            spinnerArray.add("Photo");

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = findViewById(R.id.modeSpinner);
            spinner.setAdapter(adapter);

            CameraCharacteristics c = manager.getCameraCharacteristics("0");

            // Color blindness
            spinnerArray =  new ArrayList<String>();

            spinnerArray.add("Protanopia");
            spinnerArray.add("Deuteranopia");
            spinnerArray.add("Tritanopia");

            adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner = findViewById(R.id.colorBlindnessSpinner);
            spinner.setAdapter(adapter);

            // 3D type
            spinnerArray =  new ArrayList<String>();

            spinnerArray.add("Anaglyph");
            spinnerArray.add("Side by side");
            spinnerArray.add("Wiggle (only for Photo mode)");
            spinnerArray.add("None");

            adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner = findViewById(R.id.type3DSpinner);
            spinner.setAdapter(adapter);

            // Get all possible frame sizes
            spinnerArray =  new ArrayList<String>();

            for (Size s : c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.YUV_420_888)) {
                spinnerArray.add(s.toString());
            }

            adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner = findViewById(R.id.frameSizeSpinner);
            spinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Pass parameters to CameraActivity
        ((Button)findViewById(R.id.startButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i;
                String type3D = ((Spinner) findViewById(R.id.type3DSpinner))
                        .getSelectedItem().toString();

                if (((Spinner)findViewById(R.id.modeSpinner))
                        .getSelectedItem().toString().equals("Photo")) {
                    i = new Intent(getApplicationContext(), com.pfa.PhotoActivity.class);
                    i.putExtra("3d", type3D);
                } else {
                    i = new Intent(getApplicationContext(), com.pfa.CameraActivity.class);

                    i.putExtra("mode", ((Spinner) findViewById(R.id.modeSpinner))
                            .getSelectedItem().toString());

                    i.putExtra("colorBlindness", ((Spinner) findViewById(R.id.colorBlindnessSpinner))
                            .getSelectedItem().toString());

                    i.putExtra("frameSize", ((Spinner) findViewById(R.id.frameSizeSpinner))
                            .getSelectedItem().toString());

                    i.putExtra("3d", type3D);

                    if (type3D.equals("Wiggle (only for Photo mode)")) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("Wiggle 3D can only be used with Photo mode.")
                            .setNeutralButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                        return;
                    }
                }

                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!openCVLoaded) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("Internal OpenCV library not found, exiting.")
                    .setCancelable(false)
                    .setNeutralButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finishAndRemoveTask();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /**
     * Returns true if the app was granted all the permissions. Otherwise, returns false.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }
}