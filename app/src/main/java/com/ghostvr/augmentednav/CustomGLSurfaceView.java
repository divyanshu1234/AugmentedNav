package com.ghostvr.augmentednav;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.widget.Toast;

import static android.opengl.Matrix.rotateM;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class CustomGLSurfaceView extends GLSurfaceView implements SensorEventListener {
    private boolean rendererSet = false;
    private CustomRenderer mRenderer;
    private Context context;

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    private float angle;

    private final float[] mRotationReading = new float[3];
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientationAngles = new float[3];

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
            return;
        }

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        angle = 0.0f;

    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public boolean isRendererSet(){
        return rendererSet;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, mRotationReading,
                    0, mRotationReading.length);
        }

        mSensorManager.getRotationMatrixFromVector(mRotationMatrix, mRotationReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        rotateM(mRotationMatrix, 0, angle, 0f, 0f, 1f);
        mRenderer.mAccumulatedRotationMatrix = mRotationMatrix.clone();
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

    }

}
