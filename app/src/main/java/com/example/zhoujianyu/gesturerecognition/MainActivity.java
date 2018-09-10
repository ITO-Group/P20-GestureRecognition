package com.example.zhoujianyu.gesturerecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.Touch;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    public static final int ROW_NUM = 32;
    public static final int COL_NUM = 16;
    public static final int BOTTOM_TOUCH_THR = 80;
    public static final int BOTTOM_HOLD_THR=200;
    public int screenWidth;
    public int screenHeight;
    public int capaWidth;
    public int capaHeight;
    public int window=2;
    public int capacity_data[][][] = new int[ROW_NUM][COL_NUM][window];
    public int diff_data[][] = new int[ROW_NUM][COL_NUM];




    //views
    ImageView mKeyboardView;
    ImageView mLandScapeKeyboardView;
    String last_status="camera";
    boolean is_camera_open = false;
    int count = 0;


    //camera related
    private final String TAG = getClass().getSimpleName();
    private Button mTakePhoto;

    public boolean isCameraUsebyApp() {
        Camera camera = null;
        try {
            camera = Camera.open();
            Log.e("Camera", "Status_off"); /// it was off
            Toast.makeText(this, "Now On", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Log.e("Camera", "Status_off"); /// it was on
            Toast.makeText(this, "Now Off", Toast.LENGTH_SHORT).show();
            return true;
        } finally {
            if (camera != null) camera.release();
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==-1){
            String path = "storage/emulated/0/DCIM/Camera/cam_image.jpg";
            Toast.makeText(this,"Pictured stored in "+path,Toast.LENGTH_SHORT).show();
        }
        is_camera_open = false;
    }

    public static File getTempImage() {
        File folder = new File("storage/emulated/0/DCIM/Camera");
        if(!folder.exists()){
            folder.mkdir();
        }
        File image_file = new File(folder,"cam_image.jpg");
        return image_file;
    }

    public void takePicture(){
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = getTempImage();
        intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(file));
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mKeyboardView = findViewById(R.id.imageView);
        mLandScapeKeyboardView = findViewById(R.id.imageView4);
        mLandScapeKeyboardView.setVisibility(View.INVISIBLE);
        //get internal storage read access
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        init();
        readDiffStart();
    }

    public void init(){
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        capaWidth = screenWidth/COL_NUM;
        capaHeight = screenHeight/ROW_NUM;
    }
    public void updateCapacity(short[] data){
        // moving window
        for(int w = 0;w+1<window;w++){
            for(int i = 0;i<ROW_NUM;i++){
                for(int j = 0;j<COL_NUM;j++) capacity_data[i][j][w] = capacity_data[i][j][w+1];
            }
        }
        //update most current image
        for(int i = 0;i<ROW_NUM;i++){
            for(int j = 0;j<COL_NUM;j++){
                capacity_data[i][j][window-1] = data[i*COL_NUM+j];
            }
        }
        //calculate diff
        for(int i = 0;i<ROW_NUM;i++){
            for(int j = 0;j<COL_NUM;j++){
                diff_data[i][j] = capacity_data[i][j][window-1]-capacity_data[i][j][window-2];
            }
        }
    }


    public String getGesture() {
        /** bottom_left_area:col_2-col_5
         *  bottom_right_area: col_10-col_13
         */
        boolean bottom_left = false;
        boolean bottom_right = false;
        boolean top_left = false;
        boolean top_right = false;
        for (int j = 2; j <= 5; j++) {
            if (diff_data[ROW_NUM - 1][j] > BOTTOM_TOUCH_THR || capacity_data[ROW_NUM - 1][j][window - 1] > BOTTOM_HOLD_THR) {
                bottom_left = true;
                break;
            }
        }
        for (int j = 10; j <= 13; j++) {
            if (diff_data[ROW_NUM - 1][j] > BOTTOM_TOUCH_THR || capacity_data[ROW_NUM - 1][j][window - 1] > BOTTOM_HOLD_THR) {
                bottom_right = true;
                break;
            }
        }
        for (int j = 2; j <= 5; j++) {
            if (diff_data[0][j] > BOTTOM_TOUCH_THR || capacity_data[0][j][window - 1] > BOTTOM_HOLD_THR) {
                top_left = true;
                break;
            }
        }
        for (int j = 10; j <= 13; j++) {
            if (diff_data[0][j] > BOTTOM_TOUCH_THR || capacity_data[0][j][window - 1] > BOTTOM_HOLD_THR) {
                top_right = true;
                break;
            }
        }

        boolean left_top = false;
        boolean left_bottom = false;
        boolean right_top = false;
        boolean right_bottom = false;
        for (int i = 4; i < 9; i++) {
            if (diff_data[i][0] > BOTTOM_TOUCH_THR || capacity_data[i][0][window - 1] > BOTTOM_HOLD_THR) {
                left_top = true;
                break;
            }
        }
        for (int i = 4; i < 9; i++) {
            if (diff_data[i][COL_NUM - 1] > BOTTOM_TOUCH_THR || capacity_data[i][COL_NUM - 1][window - 1] > BOTTOM_HOLD_THR) {
                right_top = true;
                break;
            }
        }
        for (int i = 22; i < 27; i++) {
            if (diff_data[i][0] > BOTTOM_TOUCH_THR || capacity_data[i][0][window - 1] > BOTTOM_HOLD_THR) {
                left_bottom = true;
                break;
            }
        }
        for (int i = 22; i < 27; i++) {
            if (diff_data[i][COL_NUM - 1] > BOTTOM_TOUCH_THR || capacity_data[i][COL_NUM - 1][window - 1] > BOTTOM_HOLD_THR) {
                right_bottom = true;
                break;
            }
        }

        if (bottom_left && !bottom_right && !top_left && !top_right) return "right_hand";
        if (!bottom_left && bottom_right && !top_left && !top_right) return "left_hand";
        if (!bottom_left && !bottom_right && !top_left && !top_right && (left_top || left_bottom || right_top || right_bottom))
            return "camera";
        else return "game";
    }
    /**
     * callback method after everytime native_lib.cpp read an image of capacity data
     * The function first convert
     * @param data: 32*16 short array
     */
    public void processDiff(short[] data) throws InterruptedException{
        count++;
        updateCapacity(data);
        String gesType = getGesture();

        Configuration cf = this.getResources().getConfiguration();
        if(cf.orientation==cf.ORIENTATION_PORTRAIT){
            mLandScapeKeyboardView.setVisibility(View.INVISIBLE);
            if(gesType.equals("right_hand")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLandScapeKeyboardView.setVisibility(View.INVISIBLE);
                        mKeyboardView.setVisibility(View.VISIBLE);
                        mKeyboardView.setImageResource(R.drawable.right);
                    }
                });
            }
            else if(gesType.equals("left_hand")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLandScapeKeyboardView.setVisibility(View.INVISIBLE);
                        mKeyboardView.setVisibility(View.VISIBLE);
                        mKeyboardView.setImageResource(R.drawable.left);
                    }
                });
            }
        }
        else{
            if(gesType.equals("game")){
                last_status="game";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLandScapeKeyboardView.setVisibility(View.VISIBLE);
                        mKeyboardView.setVisibility(View.INVISIBLE);
                    }
                });
            }
            else if(gesType.equals("camera")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLandScapeKeyboardView.setVisibility(View.VISIBLE);
                        mKeyboardView.setVisibility(View.INVISIBLE);
                    }
                });
                if(!is_camera_open){
                    is_camera_open=true;
                    takePicture();
                }
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void readDiffStart ();
    public native void readDiffStop ();
}
