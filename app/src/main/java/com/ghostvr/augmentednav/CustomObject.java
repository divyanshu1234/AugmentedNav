package com.ghostvr.augmentednav;

import android.content.Context;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by Divyanshu on 5/24/17.
 */

public class CustomObject {
    private final Context context;
    private int program;

    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private int TRIANGLE_COUNT;
    private static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer vertexCoordinateData;
    private final FloatBuffer vertexColorData;


    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    private static final String A_COLOR = "a_Color";
    private int aColorLocation;

    private static final String U_MATRIX = "u_Matrix";
    private int uMatrixLocation;

    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;


    public CustomObject(Context context, float[] tableCoordinateTriangles, boolean isVrEnabled){
        this.context = context;

        float[] tableColorTriangles = new float[tableCoordinateTriangles.length];

        for (int i = 0, j = 0; i < tableColorTriangles.length / 9; ++i, j += 9){
            tableColorTriangles[j]   = 1f;
            tableColorTriangles[j+1] = 0f;
            tableColorTriangles[j+2] = 0f;
            tableColorTriangles[j+3] = 0f;
            tableColorTriangles[j+4] = 1f;
            tableColorTriangles[j+5] = 0f;
            tableColorTriangles[j+6] = 1f;
            tableColorTriangles[j+7] = 1f;
            tableColorTriangles[j+8] = 0f;
        }

        float[] mirroredCoordinates = new float[tableCoordinateTriangles.length];
        System.arraycopy(tableCoordinateTriangles, 0, mirroredCoordinates, 0, tableCoordinateTriangles.length);

        //Mirroring Y - Coordinate
        if (!isVrEnabled)
            for (int i = 1; i < tableCoordinateTriangles.length; i += 3)
                mirroredCoordinates[i] = -mirroredCoordinates[i];


        float[] rotatedCoordinates = new float[tableCoordinateTriangles.length];
        rotateObject(rotatedCoordinates, mirroredCoordinates, 0, 0.0f, 1, 0, 0);

        TRIANGLE_COUNT = tableCoordinateTriangles.length / 9;

        vertexCoordinateData = ByteBuffer
                .allocateDirect(tableCoordinateTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexCoordinateData.put(rotatedCoordinates);

        vertexColorData = ByteBuffer
                .allocateDirect(tableColorTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexColorData.put(tableColorTriangles);


        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.simple_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);
        ShaderHelper.validateProgram(program);
        glUseProgram(program);
    }

    private void rotateObject(float[] rotatedCoordinates, float[] tableCoordinateTriangles, int offset, float a, int x, int y, int z) {
        float[] rotationMatrix = new float[16];
        Matrix.setRotateM(rotationMatrix, offset, a, x, y, z);

        for (int i = 0; i < tableCoordinateTriangles.length; i += 3){
            float[] point = new float[4];
            float[] resultPoint = new float[4];

            point[0] = tableCoordinateTriangles[i];
            point[1] = tableCoordinateTriangles[i+1];
            point[2] = tableCoordinateTriangles[i+2];
            point[3] = 1.0f;

            Matrix.multiplyMV(resultPoint, 0, rotationMatrix, 0, point, 0);
            rotatedCoordinates[i] = resultPoint[0];
            rotatedCoordinates[i+1] = resultPoint[1];
            rotatedCoordinates[i+2] = resultPoint[2];
        }
    }

    public void draw(float[] projectionMatrix){
        aColorLocation = glGetAttribLocation(program, A_COLOR);
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

        vertexCoordinateData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, 0, vertexCoordinateData);
        glEnableVertexAttribArray(aPositionLocation);

        vertexColorData.position(0);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT,
                false, 0, vertexColorData);
        glEnableVertexAttribArray(aColorLocation);

        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        glDrawArrays(GL_TRIANGLES, 0, TRIANGLE_COUNT * 3);

        glDisableVertexAttribArray(aPositionLocation);
        glDisableVertexAttribArray(aColorLocation);
    }
}