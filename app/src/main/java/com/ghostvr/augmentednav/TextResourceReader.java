package com.ghostvr.augmentednav;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class TextResourceReader {

    public static String readTextFileFromResource(Context context,
                                                  int resourceId){
        StringBuilder body = new StringBuilder();

        try{
            BufferedReader bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getResources().openRawResource(resourceId)));

            String nextLine;

            while((nextLine = bufferedReader.readLine()) != null){
                body.append(nextLine);
                body.append('\n');
            }

        } catch (IOException e){
            throw new RuntimeException("Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException e2){
            throw  new RuntimeException("Resource not found: " + resourceId, e2);
        }

        return body.toString();
    }
}
