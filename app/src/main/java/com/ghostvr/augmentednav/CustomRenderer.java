package com.ghostvr.augmentednav;

import android.content.Context;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_NICEST;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearDepthf;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glHint;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.perspectiveM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class CustomRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    CustomObject customObject;

    private static final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    private final float[] finalMatrix = new float[16];
    private final float mTempMatrix[] = new float[16];


    private final float[] tableCoordinateTriangles;
    public float eyeTranslation;
    public boolean isObjectCentered;
    public boolean isVrEnabled;

    public static float[] mAccumulatedRotationMatrix;

    public static ROSThread rosThread;
    private float[] quatArray;
    private float[] transArray;

    static {
        rosThread = new ROSThread();
        new Thread(rosThread).start();
    }

    public CustomRenderer(Context context, float[] tableCoordinateTriangles, boolean isObjectCentered, boolean isVrEnabled){
        this.context = context;
        mAccumulatedRotationMatrix = new float[16];
        eyeTranslation = 0.0f;
        this.tableCoordinateTriangles = tableCoordinateTriangles;
        this.isObjectCentered = isObjectCentered;
        this.isVrEnabled = isVrEnabled;

        quatArray = new float[4];
        transArray = new float[3];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f,0.0f,0.0f,0.0f);
        glClearDepthf(1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

        customObject = new CustomObject(context, tableCoordinateTriangles, isVrEnabled);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        perspectiveM(projectionMatrix, 0, 45, (float) width / (float) height, 0.5f, 10f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        getRotationMatrixFromQuaternions();
        getTranslationMatrix();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        setIdentityM(modelMatrix, 0);

        setIdentityM(mScaleMatrix, 0);
        scaleM(mScaleMatrix, 0, 0.075f, 0.075f, 0.075f);
        System.arraycopy(modelMatrix, 0, mTempMatrix, 0, modelMatrix.length);
        multiplyMM(modelMatrix, 0, mScaleMatrix, 0, mTempMatrix, 0);


        if (isObjectCentered){
            System.arraycopy(modelMatrix, 0, mTempMatrix, 0, modelMatrix.length);
            multiplyMM(modelMatrix, 0, mAccumulatedRotationMatrix, 0, mTempMatrix, 0);

            translateM(mTranslationMatrix, 0, 0.0f, eyeTranslation, -1.0f);
            System.arraycopy(modelMatrix, 0, mTempMatrix, 0, modelMatrix.length);
            multiplyMM(modelMatrix, 0, mTranslationMatrix, 0, mTempMatrix, 0);

        } else {
            translateM(mTranslationMatrix, 0, -4.0f, 0.0f, 0.0f);
            System.arraycopy(modelMatrix, 0, mTempMatrix, 0, modelMatrix.length);
            multiplyMM(modelMatrix, 0, mTranslationMatrix, 0, mTempMatrix, 0);

            System.arraycopy(modelMatrix, 0, mTempMatrix, 0, modelMatrix.length);
            multiplyMM(modelMatrix, 0, mAccumulatedRotationMatrix, 0, mTempMatrix, 0);
        }

        multiplyMM(finalMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        customObject.draw(finalMatrix);
    }

    private void getRotationMatrixFromQuaternions() {
        quatArray = rosThread.getQuatArray();
        float qx = quatArray[0];
        float qy = quatArray[1];
        float qz = quatArray[2];
        float qw = quatArray[3];

        mAccumulatedRotationMatrix[0] = 1 - 2*qy*qy - 2*qz*qz;
        mAccumulatedRotationMatrix[1] = 2*qx*qy - 2*qz*qw;
        mAccumulatedRotationMatrix[2] = 2*qx*qz + 2*qy*qw;
        mAccumulatedRotationMatrix[3] = 0;
        mAccumulatedRotationMatrix[4] = 2*qx*qy + 2*qz*qw;
        mAccumulatedRotationMatrix[5] = 1 - 2*qx*qx - 2*qz*qz;
        mAccumulatedRotationMatrix[6] = 2*qy*qz - 2*qx*qw;
        mAccumulatedRotationMatrix[7] = 0;
        mAccumulatedRotationMatrix[8] = 2*qx*qz - 2*qy*qw;
        mAccumulatedRotationMatrix[9] = 2*qy*qz + 2*qx*qw;
        mAccumulatedRotationMatrix[10] = 1 - 2*qx*qx - 2*qy*qy;
        mAccumulatedRotationMatrix[11] = 0;
        mAccumulatedRotationMatrix[12] = 0;
        mAccumulatedRotationMatrix[13] = 0;
        mAccumulatedRotationMatrix[14] = 0;
        mAccumulatedRotationMatrix[15] = 1;
    }

    public void getTranslationMatrix() {
        transArray = rosThread.getPointArray();

        Matrix.setIdentityM(mTranslationMatrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, 10*transArray[0], 10*transArray[1], 10*transArray[2]);
    }
}
