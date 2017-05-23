package com.ghostvr.augmentednav;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.widget.Toast;

import static android.opengl.Matrix.rotateM;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class CustomGLSurfaceView extends GLSurfaceView {
    private boolean rendererSet = false;
    private CustomRenderer mRenderer;
    private Context context;

    private float angleToNextPoint;


    public CustomGLSurfaceView(Context context) {
        super(context);
        this.context = context;
        initialize();
    }

    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initialize();
    }


    public void initialize() {
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();

        final boolean supportsES2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsES2) {
            setEGLContextClientVersion(2);
            mRenderer = new CustomRenderer(context);
            setRenderer(mRenderer);
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            rendererSet = true;
        } else {
            Toast.makeText(context,
                    "This device does not support OpenGL ES 2.0",
                    Toast.LENGTH_LONG).show();
        }

        angleToNextPoint = 0.0f;
    }


    public boolean isRendererSet(){
        return rendererSet;
    }

    public void setRotationMatrix(float[] rotationMatrix){
        mRenderer.mAccumulatedRotationMatrix = rotationMatrix.clone();
    }

    public void setAngleToNextPoint(float angleToNextPoint) {
        this.angleToNextPoint = angleToNextPoint;
    }

    public float getAngleToNextPoint() {
        return angleToNextPoint;
    }
}
