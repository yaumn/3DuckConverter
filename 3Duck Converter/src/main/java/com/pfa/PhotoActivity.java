package com.pfa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Handle the processing of an image on-disk
 */
public class PhotoActivity extends Activity {
    static final int REQUEST_CODE = 42;

    Bitmap mLeft;
    Bitmap mRight;
    Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    ((ProgressBar)findViewById(R.id.progressLoader)).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.progressMessage)).setVisibility(View.VISIBLE);
                    mUri = resultData.getData();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = null;
                            try {
                                mLeft = MediaStore.Images.Media.getBitmap(PhotoActivity.this.getContentResolver(), mUri);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Mat img = new Mat();
                            Mat dst = new Mat();
                            Utils.bitmapToMat(mLeft, img);


                            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2RGB);

                            String type3D = PhotoActivity.this.getIntent().getStringExtra("3d");
                            if (type3D.equals("Wiggle (only for Photo mode)")) {
                                int rows = img.rows();
                                int cols = img.cols();

                                Disparity2 disparity = new Disparity2();
                                disparity.init(img);
                                disparity.computeDisparity(40);

                                int scale = (int)(255.0 / (cols * Parameters.disparityScaling));
                                if (scale == 0){
                                    scale = 1;
                                }

                                int biggestEdgeDisparity = ImageConversion.computeCorrespondingImage(img, disparity.heatMap,
                                        dst, true, scale);

                                mLeft = Bitmap.createBitmap(cols - biggestEdgeDisparity, rows, Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(img.submat(0, rows,
                                        0, cols - biggestEdgeDisparity), mLeft);
                            } else {
                                ImageConversion.full3DConversion(img, dst, type3D);
                            }

                            mRight = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(dst, mRight);

                            if (type3D.equals("Wiggle (only for Photo mode)")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageView iv = findViewById(R.id.imageView);
                                        iv.postDelayed(new Runnable() {
                                            int i = 0;
                                            @Override
                                            public void run() {
                                                ImageView iv = findViewById(R.id.imageView);
                                                iv.setImageBitmap(i++ % 2 == 0 ? mLeft : mRight);
                                                iv.postDelayed(this, 350);
                                            }
                                        }, 350);
                                        ((ProgressBar) findViewById(R.id.progressLoader)).setVisibility(View.INVISIBLE);
                                        ((TextView) findViewById(R.id.progressMessage)).setVisibility(View.INVISIBLE);
                                        iv.setVisibility(View.VISIBLE);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageView iv = findViewById(R.id.imageView);
                                        iv.setImageBitmap(mRight);
                                        ((ProgressBar) findViewById(R.id.progressLoader)).setVisibility(View.INVISIBLE);
                                        ((TextView) findViewById(R.id.progressMessage)).setVisibility(View.INVISIBLE);
                                        iv.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    }).start();
                }
            } else {
                finish();
            }
        }
    }
}
