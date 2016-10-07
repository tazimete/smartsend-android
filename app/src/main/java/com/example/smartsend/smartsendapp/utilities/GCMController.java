package com.example.smartsend.smartsendapp.utilities;

import android.content.Context;

import com.example.smartsend.smartsendapp.R;

/**
 * Created by pict-xx on 10/2/2016.
 */

public class GCMController {
    private String projectNumber;

    GCMController(){

    }

    GCMController(Context context){
        this.projectNumber = context.getString(R.string.gcm_project_number);
    }

    //get gcm project number
    public String getProjectNumber(){
        return  this.projectNumber;
    }

    //get gcm project number
    public static String getProjectNumber(Context context){
        return context.getString(R.string.gcm_project_number);
    }


}
