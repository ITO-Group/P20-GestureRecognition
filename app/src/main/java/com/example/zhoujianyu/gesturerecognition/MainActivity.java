package com.example.zhoujianyu.gesturerecognition;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    public static final int ROW_NUM = 32;
    public static final int COL_NUM = 16;
    public static final int TOUCH_THR = 500;
    public int screenWidth;
    public int screenHeight;
    public int capaWidth;
    public int capaHeight;
    public int capacity_data[][] = new int[ROW_NUM][COL_NUM];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        for(int i = 0;i<ROW_NUM;i++){
            for(int j = 0;j<COL_NUM;j++) capacity_data[i][j] = data[i*COL_NUM+j];
        }
    }


    public int getGesture(){
        boolean left_top = true; boolean right_top = true; boolean left_bottom = true; boolean right_bottom = true;
        for(int i =4;i<7;i++){
            if(capacity_data[i][0]<TOUCH_THR) {
                left_top = false;
            }
            if(capacity_data[i][COL_NUM-1]<TOUCH_THR){
                right_top = false;
            }
        }
        for(int i = ROW_NUM-6;i<ROW_NUM-3;i++){
            if(capacity_data[i][0]<TOUCH_THR) left_bottom=false;
            if(capacity_data[i][COL_NUM-1]<TOUCH_THR) right_bottom = false;
        }
        if(left_top&&right_top&&left_bottom&&right_bottom) return 0;
        else return -1;

    }
    /**
     * callback method after everytime native_lib.cpp read an image of capacity data
     * The function first convert
     * @param data: 32*16 short array
     */
    public void processDiff(short[] data) throws InterruptedException{
        updateCapacity(data);
        int gesType = getGesture();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void readDiffStart ();
    public native void readDiffStop ();
}
