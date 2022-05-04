// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.ncnnyolov5;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;
    public static final int REQUEST_WRITE_EXTERNAL = 101;

    private NcnnYolov5 ncnnyolov5 = new NcnnYolov5();
    private int facing = 0;

//    private Spinner spinnerModel;
//    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;
    private LinearLayout parentView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraView = (SurfaceView) findViewById(R.id.cameraview);

        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int new_facing = 1 - facing;

                ncnnyolov5.closeCamera();

                ncnnyolov5.openCamera(new_facing);

                facing = new_facing;
            }
        });

//        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
//        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
//            {
//                if (position != current_model)
//                {
//                    current_model = position;
//                    reload();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> arg0)
//            {
//            }
//        });

//        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
//        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
//            {
//                if (position != current_cpugpu)
//                {
//                    current_cpugpu = position;
//                    reload();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> arg0)
//            {
//            }
//        });
        parentView = (LinearLayout) findViewById(R.id.parentView);

        Button buttonGetCorners = (Button) findViewById(R.id.buttonGetCorners);
        buttonGetCorners.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                capturePicture(cameraView);
            }
        });
        reload();
    }

    private void reload()
    {
        boolean ret_init = ncnnyolov5.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "ncnnyolov5 loadModel failed");
        }
        ncnnyolov5.getCorners(ncnnyolov5.corners);
        ncnnyolov5.printCorners();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        ncnnyolov5.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL);
        }

        ncnnyolov5.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        ncnnyolov5.closeCamera();
    }

    private Point getAbsoluteCoordinate(Point point, Size rgb, Size screen ){
        if(rgb.getWidth() > 0 && rgb.getHeight() > 0){
            point.x = point.x * screen.getWidth() / rgb.getWidth();
            point.y = point.y * screen.getHeight() / rgb.getHeight();
        } else {
            point.x = 0;
            point.y = 0;
        }

        Log.d("ncnn_corners", "Rect Absolute x: " + point.x + " y: " + point.y );
        return point;
    }

    private boolean missCorner(Point point, String label){
        if( point.x < 0 && point.y < 0){
            Log.d("ncnn_corners_miss", "missing point - label: " + label );
            return  false;
        }
        return true;
    }
    private boolean hasFace(){
        if(ncnnyolov5.hasFace() == true){
            return true;
        }else
            return false;
    }
    private void capturePicture(SurfaceView surfaceView) {
        Bitmap bmp = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
        PixelCopy.request(surfaceView, bmp, i -> {
            NcnnYolov5.corners = ncnnyolov5.getCorners(NcnnYolov5.corners);
            NcnnYolov5.printCorners();
            Point topLeft = new Point((int) NcnnYolov5.corners[0],(int) NcnnYolov5.corners[1]);
            Point topRight = new Point((int) NcnnYolov5.corners[2],(int) NcnnYolov5.corners[3]);
            Point bottomLeft = new Point((int) NcnnYolov5.corners[4],(int) NcnnYolov5.corners[5]);
            Point bottomRight = new Point((int) NcnnYolov5.corners[6],(int) NcnnYolov5.corners[7]);
            int rgb_w = NcnnYolov5.corners[8];
            int rgb_h = NcnnYolov5.corners[9];

            Log.d("ncnn_corners", "rgb width " + rgb_w + " heigth "+ rgb_h);

            Size rgb_size = new Size(rgb_w, rgb_h);
            Size screen_size = new Size(bmp.getWidth(), bmp.getHeight());

            topLeft = getAbsoluteCoordinate(topLeft, rgb_size, screen_size);
            topRight = getAbsoluteCoordinate(topRight, rgb_size, screen_size);
            bottomLeft = getAbsoluteCoordinate(bottomLeft, rgb_size, screen_size);
            bottomRight = getAbsoluteCoordinate(bottomRight, rgb_size, screen_size);

            boolean checkFace = hasFace();
            if(checkFace){
                Log.d("ncnn_corners", "IDcard have a face ");
            }else {
                Log.d("ncnn_corners", "IDcard haven't a face ");
            }
            boolean checkMiss = missCorner(topLeft, "Top left") &&
                    missCorner(topRight, "Top right") &&
                    missCorner(bottomLeft, "Bottom left") &&
                    missCorner(bottomRight, "Bottom right");
            Log.d("ncnn_corners", "check miss topleft " + missCorner(topLeft, "Top left"));
            Log.d("ncnn_corners", "check miss topRight " + missCorner(topRight, "Top right"));
            Log.d("ncnn_corners", "check miss bottomLeft " + missCorner(bottomLeft, "Bottom left"));
            Log.d("ncnn_corners", "check miss bottomRight " + missCorner(bottomRight, "Bottom right"));
            Log.d("ncnn_corners", "check miss " + checkMiss);
            if(checkMiss){
                Bitmap crop_bmp = CropUtils.crop(bmp,topLeft,topRight,bottomLeft,bottomRight,this);

                String mPath = Environment.getExternalStorageDirectory().toString() + "/" + "1234567890" + ".jpg";
                String mPath_crop = Environment.getExternalStorageDirectory().toString() + "/" + "1234567890_crop" + ".jpg";
                saveScreenshot(mPath, bmp);
                saveScreenshot(mPath_crop, crop_bmp);

                Log.d("ncnn_corners", "bitmap width " + bmp.getWidth() + " heigth "+ bmp.getHeight());
            }

        }, new Handler(Looper.getMainLooper()));
    }

    private void saveScreenshot(String mPath, Bitmap bitmap){
        File imageFile = new File(mPath);

        try{
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d("ncnn_corners", "mpath" + mPath);
            //MediaStore.Images.Media.insertImage(getContentResolver(),imageFile.getAbsolutePath(),imageFile.getName(),imageFile.getName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
class CropUtils {
    private static int WIDTH_BLOCK = 40;
    private static int HEIGHT_BLOCK = 40;
    public static Bitmap crop(Bitmap bitmap, Point topLeft, Point topRight, Point bottomLeft, Point bottomRight, Context context) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth()+1, bitmap.getHeight()+1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
// 1. draw path
        Path path = new Path();
        path.moveTo(topLeft.x, topLeft.y);
        path.lineTo(topRight.x, topRight.y);
        path.lineTo(bottomRight.x, bottomRight.y);
        path.lineTo(bottomLeft.x, bottomLeft.y);
        path.close();
        canvas.drawPath(path, paint);

// 2. draw original bitmap
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

// 3. cut

        int my_w = topLeft.x - bottomLeft.x;
        int my_h = topRight.y - topLeft.y;

        Log.d("ncnn_corners", "Rect my wh " +my_w+ ", " + my_h);

        Rect cropRect = new Rect(
                Math.min(topLeft.x, bottomLeft.x),
                Math.min(topLeft.y, topRight.y),
                Math.max(bottomRight.x, topRight.x),
                Math.max(bottomRight.y, bottomLeft.y));

        Log.d("ncnn_corners", "Rect cut " + cropRect.left + ", " + cropRect.top + ", " + cropRect.right + ", " + cropRect.bottom);
        if(cropRect.width() <= 0 || cropRect.height() <=0 ){
            Toast.makeText(context, "Anh chup sai hướng!", Toast.LENGTH_SHORT);
        } else {
            Bitmap cut = Bitmap.createBitmap(
                    output,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height()
            );
            Log.d("ncnn_corners", "Rect crop wh " + cropRect.width()+ ", " + cropRect.height());
            return cut;
        }
        return bitmap;
    }

}
