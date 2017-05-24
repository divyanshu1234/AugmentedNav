package com.ghostvr.augmentednav;

import android.content.Context;
import android.opengl.GLSurfaceView;

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
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class CustomRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    Arrow mArrow;

    private static final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    private final float[] finalMatrix = new float[16];

    public float[] mAccumulatedRotationMatrix;
    public float eyeTranslation;

    public CustomRenderer(Context context){
        this.context = context;
        mAccumulatedRotationMatrix = new float[16];
        eyeTranslation = 0.0f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f,0.0f,0.0f,0.0f);
        glClearDepthf(1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

        mArrow = new Arrow(context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

//        perspectiveM(projectionMatrix, 0, 45, (float) width / (float) height, 1f, 10f);

        perspectiveM(projectionMatrix, 0, 45, (float) width / (float) height, 0.5f, 10f);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        //set up the matrix
        float mTempMatrix[];

        setIdentityM(modelMatrix, 0);

        setIdentityM(mScaleMatrix, 0);
//        scaleM(mScaleMatrix, 0, 0.2f, 0.2f, 0.2f);

        scaleM(mScaleMatrix, 0, 0.1f, 0.1f, 0.1f);

        mTempMatrix = modelMatrix.clone();
        multiplyMM(modelMatrix, 0, mScaleMatrix, 0, mTempMatrix, 0);


        mTempMatrix = modelMatrix.clone();
        multiplyMM(modelMatrix, 0, mAccumulatedRotationMatrix, 0, mTempMatrix, 0);


        setIdentityM(mTranslationMatrix, 0);
        translateM(mTranslationMatrix, 0, 0f, eyeTranslation, -1.0f);

        mTempMatrix = modelMatrix.clone();
        multiplyMM(modelMatrix, 0, mTranslationMatrix, 0, mTempMatrix, 0);

        multiplyMM(finalMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        mArrow.draw(finalMatrix);
    }

}
