package com.ghostvr.augmentednav;

import android.content.Context;

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
 * Created by Divyanshu on 3/8/17.
 */

public class Arrow {
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


    public Arrow(Context context){
        this.context = context;

        float[] tableCoordinateTriangles = {
                // Order of coordinates: X, Y, Z

                -0.5f,-2f,0.5f,   0.5f,-2f,0.5f,   0.5f,0f,0.5f,
                 0.5f,0f,0.5f,   -0.5f,0f,0.5f,   -0.5f,-2f,0.5f,
                 0.5f,0f,0.5f,    1.5f,0f,0.5f,    0f,2f,0.5f,
                 0.5f,0f,0.5f,    0f,2f,0.5f,     -1.5f,0f,0.5f,
                -0.5f,0f,-0.5f,  -1.5f,0f,-0.5f,   0f,2f,-0.5f,
                -0.5f,0f,-0.5f,   0f,2f,-0.5f,     1.5f,0f,-0.5f,
                -0.5f,0f,-0.5f,   0.5f,0f,-0.5f,   0.5f,-2f,-0.5f,
                -0.5f,0f,-0.5f,   0.5f,-2f,-0.5f, -0.5f,-2f,-0.5f,
                 0.5f,-2f,-0.5f,  0.5f,-2f,0.5f,  -0.5f,-2f,0.5f,
                 0.5f,-2f,-0.5f, -0.5f,-2f,0.5f,  -0.5f,-2f,-0.5f,
                 0.5f,0f,-0.5f,   0.5f,0f,0.5f,    0.5f,-2f,0.5f,
                 0.5f,0f,-0.5f,   0.5f,-2f,0.5f,   0.5f,-2f,-0.5f,
                 1.5f,0f,-0.5f,   1.5f,0f,0.5f,    0.5f,0f,0.5f,
                 1.5f,0f,-0.5f,   0.5f,0f,0.5f,    0.5f,0f,-0.5f,
                 0f,2f,-0.5f,     0f,2f,0.5f,      1.5f,0f,0.5f,
                 0f,2f,-0.5f,     1.5f,0f,0.5f,    1.5f,0f,-0.5f,
                -1.5f,0f,-0.5f,  -1.5f,0f,0.5f,    0f,2f,0.5f,
                -1.5f,0f,-0.5f,   0f,2f,0.5f,      0f,2f,-0.5f,
                -0.5f,0f,-0.5f,  -0.5f,0f,0.5f,   -1.5f,0f,0.5f,
                -0.5f,0f,-0.5f,  -1.5f,0f,0.5f,   -1.5f,0f,-0.5f,
                -0.5f,-2f,-0.5f, -0.5f,-2f,0.5f,  -0.5f,0f,0.5f,
                -0.5f,-2f,-0.5f, -0.5f,0f,0.5f,   -0.5f,0f,-0.5f
        };

        float[] tableColorTriangles = new float[tableCoordinateTriangles.length];

        for (int i = 0, j = 0; i < tableColorTriangles.length / 9; ++i, j += 9){
            tableColorTriangles[j]   = 1f;
            tableColorTriangles[j+1] = 0f;
            tableColorTriangles[j+2] = 0f;
            tableColorTriangles[j+3] = 0f;
            tableColorTriangles[j+4] = 1f;
            tableColorTriangles[j+5] = 0f;
            tableColorTriangles[j+6] = 0f;
            tableColorTriangles[j+7] = 0f;
            tableColorTriangles[j+8] = 1f;
        }

        TRIANGLE_COUNT = tableCoordinateTriangles.length / 9;

        vertexCoordinateData = ByteBuffer
                .allocateDirect(tableCoordinateTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexCoordinateData.put(tableCoordinateTriangles);

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

