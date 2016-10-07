package com.example.smartsend.smartsendapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RiderDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private String serverUrl;
    private Toolbar toolbar;
    private ToggleButton tbRiderStatus;
    private GoogleCloudMessaging gcm = null;
    private String projectNumber;
    private String deviceRegIdForGCM = null;
    private Context ctx = this;
    private ProgressDialog pDialog;
    private ConnectivityDetector connectivityDetector;
    private UserLocalStore sessionManager;
    private Rider loggedInRider;
    private  NavigationView navigationView;
    private ImageView profilePicture;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_dashboard);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        sessionManager = new UserLocalStore(ctx);
        loggedInRider = sessionManager.getLogedInRider();
        projectNumber = GCMController.getProjectNumber(this);
        serverUrl = ServerManager.getServerUrl(this);

        // Progress dialog
        pDialog = new ProgressDialog(RiderDashboardActivity.this);
        connectivityDetector = new ConnectivityDetector(getBaseContext());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view_rider_dashboard);
        navigationView.setNavigationItemSelectedListener(this);
        //Set default item selected on nivigation
        navigationView.getMenu().performIdentifierAction(R.id.nav_rider_dashboard, 0);

        //Set image and name to navigation drawer header
        View navHeader = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        profilePicture = (ImageView) navHeader.findViewById(R.id.ivprofilePicture);
        tvName = (TextView) navHeader.findViewById(R.id.tvName);
        Picasso.with(this).load(getString(R.string.server_url)+"/static/images/profile-pictures/"+loggedInRider.getProfilePicture()).into(profilePicture);
        tvName.setText(loggedInRider.getName());

        //Show Current Location
        SmartSendLocationManager ssLocationManager = new SmartSendLocationManager(getApplicationContext(),60000, 10);
        SmartSendLocationListener ssLocationListener = new SmartSendLocationListener(getApplicationContext());
        ssLocationManager.setLocationManagerAndListener();

        //Background Task for registering
        new AsyncTask() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showDialog();
            }

            @Override
            protected Object doInBackground(Object[] params) {

                try {
                    if(gcm == null){
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }

                    if(gcm != null){
                        deviceRegIdForGCM = gcm.register(projectNumber);
                    }

                    if(deviceRegIdForGCM != null){
                        registerRiderDevice( deviceRegIdForGCM );
                        //Toast.makeText(getApplicationContext(), "Device Registered : "+deviceRegIdForGCM, Toast.LENGTH_LONG).show();
                    }else{
                        // Toast.makeText(getApplicationContext(), "Device Registration Failed: "+deviceRegIdForGCM, Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    //Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                hideDialog();
            }
        }.execute(null,null,null);
        //End of registering rider device

    }//End of onCreate

    //When click on drawer item
    //Navigation drawer
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_rider_dashboard) {
            // Handle the  action
            RiderDashboardFragment riderDashboardFragment =  new RiderDashboardFragment();
            getFragmentManager().beginTransaction().replace(R.id.flMain, riderDashboardFragment).addToBackStack(null).commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Register Device for GCM  service
    public void registerRiderDevice(String projectNumber){
        String MSG;

        // Tag used to cancel the request
        String tag_string_req = "req_login";
        String serverAddress = serverUrl+"/rest_controller/register_rider_device/"+loggedInRider.getId()+"/"+deviceRegIdForGCM;
        Map<String, String> params = new HashMap<String, String>();

        //Show progress dialog
        //showDialog();

        JsonObjectRequest registerRiderDeviceRequest = new JsonObjectRequest(Request.Method.POST,
                serverAddress, new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {

                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){
                        String successMessage = jObj.getString("success_message");
                        // return outlets;
                    }else{
                        String errorMessage = jObj.getString("error_message");
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

                //hideDialog();

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //hideDialog();
                Log.e("Login Error", "Login Error: " + error.getMessage());
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(registerRiderDeviceRequest);

    }//End of registerRiderDevice

    //Show Diaslog
    private void showDialog() {
        //if (!pDialog.isShowing()) {
            pDialog.setMessage("Please Wait....");
            pDialog.setTitle("Processing");
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
