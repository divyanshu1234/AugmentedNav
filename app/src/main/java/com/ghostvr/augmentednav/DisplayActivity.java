package com.ghostvr.augmentednav;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import static android.opengl.Matrix.rotateM;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class DisplayActivity extends AppCompatActivity {

    private CustomGLSurfaceView glsv_left;
    private CustomGLSurfaceView glsv_right;
    private boolean left_rendererSet = false;
    private boolean right_rendererSet = false;

    private boolean isVrEnabled;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(params);

        CustomGLSurfaceView.tableCoordinateTriangles = getIntent().getFloatArrayExtra("tableCoordinateTriangles");
        CustomGLSurfaceView.isObjectCentered = getIntent().getBooleanExtra("camera_mode", true);
        isVrEnabled = getIntent().getBooleanExtra("mode", true);
        CustomGLSurfaceView.isVrEnabled = isVrEnabled;

        setContentView(R.layout.activity_display);
        glsv_left = (CustomGLSurfaceView) findViewById(R.id.glsv_left);
        glsv_right = (CustomGLSurfaceView) findViewById(R.id.glsv_right);
        glsv_left.setEyeTranslation(-0.032f);
        glsv_right.setEyeTranslation(0.032f);
        left_rendererSet = glsv_left.isRendererSet();
        right_rendererSet = glsv_right.isRendererSet();

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (left_rendererSet) {
            glsv_left.onResume();
        }

        if (right_rendererSet){
            glsv_right.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (left_rendererSet) {
            glsv_left.onPause();
        }

        if (right_rendererSet){
            glsv_right.onPause();
        }

    }
}