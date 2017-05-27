package com.ghostvr.augmentednav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FetchFileActivity extends AppCompatActivity {

    Button b_select_file;
    Button b_view_object;
    TextView tv_file_data;
    EditText et_scaling_factor;
    RadioButton rb_vr_mode;
    RadioButton rb_object_centered;

    float[] finalTableCoordinateTriangles;


    private static final int READ_REQUEST_CODE = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch_file);

        b_select_file = (Button) findViewById(R.id.b_select_file);
        b_view_object = (Button) findViewById(R.id.b_view_object);
        tv_file_data = (TextView) findViewById(R.id.tv_file_data);
        et_scaling_factor = (EditText) findViewById(R.id.et_scaling_factor);
        rb_vr_mode = (RadioButton) findViewById(R.id.rb_vr_mode);
        rb_object_centered = (RadioButton) findViewById(R.id.rb_object_centered);

        b_select_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            if(data != null){
                uri = data.getData();
                String extension = showMetaData(uri);
                if (extension.equals(".stl")){
                    float[] tableCoordinateTriangles = new float[0];

                    try {
                        tableCoordinateTriangles = getTriangleCoordinates(uri);
                        tv_file_data.append("\n" + tableCoordinateTriangles.length / 9);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (tableCoordinateTriangles.length == 0){
                        tv_file_data.setText("Invalid File");
                    }
                    else{
                        final float[]tempCoordinates = tableCoordinateTriangles;
                        b_view_object.setVisibility(View.VISIBLE);
                        b_view_object.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finalTableCoordinateTriangles = scaleCoordinates(tempCoordinates);

                                Intent intent = new Intent(FetchFileActivity.this, DisplayActivity.class);
                                intent.putExtra("tableCoordinateTriangles", finalTableCoordinateTriangles);
                                intent.putExtra("camera_mode", rb_object_centered.isChecked());
                                intent.putExtra("mode", rb_vr_mode.isChecked());
                                startActivity(intent);
                            }
                        });
                    }
                }
                else {
                    tv_file_data.setText("Invalid File");
                }
            }
        }
    }

    private String showMetaData(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String fileName = "";

        if (cursor != null && cursor.moveToFirst()){
            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            String size = null;
            if(!cursor.isNull(sizeIndex))
                size = cursor.getString(sizeIndex);
            else
                size = "Unknown";

            tv_file_data.setText("Name - " + fileName + "\nSize - " + size);
        }
        cursor.close();

        return fileName.substring(fileName.lastIndexOf("."));
    }

    private float[] getTriangleCoordinates(Uri uri) throws IOException {
        List<Float> coordinateList = new ArrayList<>();

        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null){
            String[] words = (line).trim().split(" ");

            if(words.length != 0){
                if(words[0].equals("vertex")){
                    coordinateList.add(Float.parseFloat(words[1]));
                    coordinateList.add(Float.parseFloat(words[2]));
                    coordinateList.add(Float.parseFloat(words[3]));
                }

            }
        }

        float[] tableCoordinateTriangles = new float[coordinateList.size()];

        for (int i = 0; i < tableCoordinateTriangles.length; ++i)
            tableCoordinateTriangles[i] = coordinateList.get(i);

        return tableCoordinateTriangles;
    }

    private float[] scaleCoordinates(float[] tableCoordinateTriangles) {
        float largestValue = 1.0f;

        for (int i = 0; i < tableCoordinateTriangles.length; ++i){
            if(tableCoordinateTriangles[i] > largestValue){
                largestValue = tableCoordinateTriangles[i];
            }
        }

        float scalingFactor = 1.0f;
        try {
            scalingFactor = Float.parseFloat(et_scaling_factor.getText().toString());
        } catch (Exception e){
            scalingFactor = 1.0f;
        }
        for (int i = 0; i < tableCoordinateTriangles.length; ++i)
            tableCoordinateTriangles[i] = scalingFactor * tableCoordinateTriangles[i] / largestValue;

        return tableCoordinateTriangles;
    }
}
