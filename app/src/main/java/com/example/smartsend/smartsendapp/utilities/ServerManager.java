package com.example.smartsend.smartsendapp.utilities;

import android.content.Context;

import com.example.smartsend.smartsendapp.R;

/**
 * Created by pict-xx on 10/2/2016.
 */

public class ServerManager {
    private String serverUrl;

    ServerManager(){

    }

    ServerManager(Context context){
        serverUrl = context.getString(R.string.server_url);
    }

    public String getServerUrl(){
        return  this.serverUrl;
    }

    public static String getServerUrl(Context context){
        return  context.getString(R.string.server_url);
    }
}
