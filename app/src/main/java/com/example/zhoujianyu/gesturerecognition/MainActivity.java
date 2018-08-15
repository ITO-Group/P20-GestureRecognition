package com.example.zhoujianyu.gesturerecognition;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.Touch;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    public static final int ROW_NUM = 32;
    public static final int COL_NUM = 16;
    public static final int BOTTOM_TOUCH_THR = 80;
    public int screenWidth;
    public int screenHeight;
    public int capaWidth;
    public int capaHeight;
    public int window=2;
    public int capacity_data[][][] = new int[ROW_NUM][COL_NUM][window];
    public int diff_data[][] = new int[ROW_NUM][COL_NUM];


    //views
//    TextView textView;
    ImageView mKeyboardView;
    ImageView mLandScapeKeyboardView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        textView = findViewById(R.id.sample_text);
//        textView.setText("portrait");
        mKeyboardView = findViewById(R.id.imageView);
        mLandScapeKeyboardView = findViewById(R.id.imageView4);
        mLandScapeKeyboardView.setVisibility(View.INVISIBLE);
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


    public String getGesture(){
        /** bottom_left_area:col_2-col_5
         *  bottom_right_area: col_10-col_13
         */
        boolean bottom_left = false; boolean bottom_right = false;
        for(int j = 2;j<=5;j++){
            if(diff_data[ROW_NUM-1][j]>BOTTOM_TOUCH_THR) {
                bottom_left = true;
                break;
            }
        }
        for(int j = 10;j<=13;j++){
            if(diff_data[ROW_NUM-1][j]>BOTTOM_TOUCH_THR){
                bottom_right = true;
                break;
            }
        }
        if(bottom_left && !bottom_right) return "right_hand";
        if(!bottom_left && bottom_right) return "left_hand";
        if(bottom_left && bottom_right)  return "game";
        else{
            return "camera";
        }
    }
    /**
     * callback method after everytime native_lib.cpp read an image of capacity data
     * The function first convert
     * @param data: 32*16 short array
     */
    public void processDiff(short[] data) throws InterruptedException{
        updateCapacity(data);
        String gesType = getGesture();
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
        else if(gesType.equals("game")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLandScapeKeyboardView.setVisibility(View.VISIBLE);
                    mKeyboardView.setVisibility(View.INVISIBLE);
                }
            });
        }

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void readDiffStart ();
    public native void readDiffStop ();
}
