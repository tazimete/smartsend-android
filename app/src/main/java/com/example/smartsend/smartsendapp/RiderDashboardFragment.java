package com.example.smartsend.smartsendapp;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsend.smartsendapp.utilities.GCMController;
import com.example.smartsend.smartsendapp.utilities.Rider;
import com.example.smartsend.smartsendapp.utilities.ServerManager;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pict-xx on 10/3/2016.
 */

public class RiderDashboardFragment extends Fragment {

    private String serverUrl;
    private Toolbar toolbar;
    private ToggleButton tbRiderStatus;
    private GoogleCloudMessaging gcm = null;
    private String projectNumber;
    private String deviceRegIdForGCM = null;
    private Context ctx;
    private ProgressDialog pDialog;
    private ConnectivityDetector connectivityDetector;
    private UserLocalStore sessionManager;
    private Rider loggedInRider;
    private TextView tvRiderProfileLoginTime, tvRiderProfileLocation, tvRiderProfileOrder, btnProfileLoginTime,
            btnProfileDuty, btnProfileOrder;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View riderDashboardFragment = inflater.inflate(R.layout.layout_rider_dashboard_fragment, container,false);
        ctx = getActivity();

        //initialize objects
        tbRiderStatus = (ToggleButton) riderDashboardFragment.findViewById(R.id.tbRiderStatus);
        tvRiderProfileLoginTime = (TextView) riderDashboardFragment.findViewById(R.id.tvProfileLogin);
        tvRiderProfileLocation = (TextView) riderDashboardFragment.findViewById(R.id.tvProfileDuty);
        tvRiderProfileOrder = (TextView) riderDashboardFragment.findViewById(R.id.tvProfileOrder);
        btnProfileLoginTime = (TextView) riderDashboardFragment.findViewById(R.id.btnProfileLoginTime);
        btnProfileDuty = (TextView) riderDashboardFragment.findViewById(R.id.btnProfileDuty);
        btnProfileOrder = (TextView) riderDashboardFragment.findViewById(R.id.btnProfileOrder);

        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        sessionManager = new UserLocalStore(ctx);
        loggedInRider = sessionManager.getLogedInRider();
        projectNumber = GCMController.getProjectNumber(getActivity());
        serverUrl = ServerManager.getServerUrl(getActivity());

        //Dialog
        pDialog = new ProgressDialog(getActivity());
        connectivityDetector = new ConnectivityDetector(getActivity());

        //Change rider profile state when toggle button is clicked
        tbRiderStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    setRiderStatus(1);
                    activateRiderProfile();
                }else{
                    setRiderStatus(0);
                    inactiveRiderProfile();
                }
            }
        });

        return riderDashboardFragment;
    }


    //Check if rider is active or not
    public void  getRiderStatus(){
        String MSG;
        //final int[] riderStatus = new int[1];

        // Tag used to cancel the request
        final String[] tag_string_req = {"req_login"};

        //Show progress dialog
        showDialog();

        String serverAddress = serverUrl+"/rest_controller/get_rider_status/"+loggedInRider.getId();
        Map<String, String> params = new HashMap<String, String>();

        JsonObjectRequest getRiderStatusRequest = new JsonObjectRequest(Request.Method.POST,
                serverAddress, new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {
                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){
                        String successMessage = jObj.getString("success_message");
                        int riderStatus = jObj.getInt("rider_status");

                        //Check if rider is active or inactive
                        if(riderStatus == 0){
                            inactiveRiderProfile();
                        }else if(riderStatus == 1){
                            activateRiderProfile();
                        }

                        Toast.makeText(ctx, "Success Message : "+successMessage, Toast.LENGTH_LONG).show();
                    }else{
                        String errorMessage = jObj.getString("error_message");
                        Toast.makeText(ctx, "Error Message : "+errorMessage, Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(ctx, "JSON Exception : "+e, Toast.LENGTH_LONG).show();
                }

                hideDialog();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                Log.e("Login Error", "Login Error: " + error.getMessage());
                Toast.makeText(ctx, "Error Response  : "+error, Toast.LENGTH_LONG).show();
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(getRiderStatusRequest);

        // return riderStatus[0];

    }//End of checkRiderStatus


    //Check if rider is active or not
    public void  setRiderStatus(int status){
        String MSG;

        // Tag used to cancel the request
        final String[] tag_string_req = {"req_login"};

        //Show progress dialog
        showDialog();

        String serverAddress = serverUrl+"/rest_controller/set_rider_status/"+loggedInRider.getId()+"/"+status;

        Map<String, String> params = new HashMap<String, String>();

        JsonObjectRequest setRiderStatusRequest = new JsonObjectRequest(Request.Method.POST,
                serverAddress, new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {
                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){
                        String successMessage = jObj.getString("success_message");
                        Toast.makeText(ctx, "Success Message : "+successMessage, Toast.LENGTH_LONG).show();
                    }else{
                        String errorMessage = jObj.getString("error_message");
                        Toast.makeText(ctx, "Error Message : "+errorMessage, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(ctx, "JSON Exception : "+e, Toast.LENGTH_LONG).show();
                }

                hideDialog();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                Log.e("Login Error", "Login Error: " + error.getMessage());
                Toast.makeText(ctx, "Error Response  : "+error, Toast.LENGTH_LONG).show();
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(setRiderStatusRequest);

        // return riderStatus[0];

    }//End of checkRiderStatus


    //Inactive rider profile
    public void inactiveRiderProfile(){
        btnProfileLoginTime.setBackgroundResource(R.drawable.bg_profile_item_inactive);
        btnProfileLoginTime.setTextColor(Color.parseColor("#514E4D"));
        btnProfileDuty.setBackgroundResource(R.drawable.bg_profile_item_inactive);
        btnProfileDuty.setTextColor(Color.parseColor("#514E4D"));
        btnProfileOrder.setBackgroundResource(R.drawable.bg_profile_item_inactive);
        btnProfileOrder.setTextColor(Color.parseColor("#514E4D"));
        btnProfileOrder.setWidth(400);
    }

    //Active rider profile
    public void activateRiderProfile(){
        btnProfileLoginTime.setBackgroundResource(R.drawable.bg_profile_item);
        btnProfileLoginTime.setTextColor(Color.parseColor("#ffffff"));
        btnProfileDuty.setBackgroundResource(R.drawable.bg_profile_item);
        btnProfileDuty.setTextColor(Color.parseColor("#ffffff"));
        btnProfileOrder.setBackgroundResource(R.drawable.bg_profile_item);
        btnProfileOrder.setTextColor(Color.parseColor("#ffffff"));
        btnProfileOrder.setWidth(400);
    }


    //Show Diaslog
    private void showDialog() {
        //if (!pDialog.isShowing()) {
        pDialog.setMessage("Please Wait....");
        pDialog.setTitle("Proccessing");
        pDialog.show();
        //}
    }

    //Hide Dialog
    private void hideDialog() {
        //if (pDialog.isShowing()) {
        pDialog.dismiss();
        //}
    }
}
