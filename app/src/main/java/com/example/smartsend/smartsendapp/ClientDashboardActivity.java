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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsend.smartsendapp.utilities.Client;
import com.example.smartsend.smartsendapp.utilities.GCMController;
import com.example.smartsend.smartsendapp.utilities.ServerManager;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ClientDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private GoogleCloudMessaging gcm;
    private String projectNumber;
    private String deviceRegIdForGCM = null;
    private Context ctx = this;
    private ProgressDialog pDialog;
    private ConnectivityDetector connectivityDetector;
    private UserLocalStore sessionManager;
    private Client loggedInClient;
    private String serverUrl;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ImageView profilePicture;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_dash_board);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sessionManager = new UserLocalStore(ctx);
        loggedInClient = sessionManager.getLogedInClient();
        projectNumber = GCMController.getProjectNumber(this);
        serverUrl = ServerManager.getServerUrl(this);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        connectivityDetector = new ConnectivityDetector(getBaseContext());

        //Setting drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view_client_dashboard);
        navigationView.setNavigationItemSelectedListener(this);
        //Set default item selected on nivigation
        navigationView.getMenu().performIdentifierAction(R.id.nav_client_dashboard, 0);

        //Set image and name to navigation drawer header
        View navHeader = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        profilePicture = (ImageView) navHeader.findViewById(R.id.ivprofilePicture);
        tvName = (TextView) navHeader.findViewById(R.id.tvName);
        tvName.setText(loggedInClient.getCompanyName());

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

                    if(!deviceRegIdForGCM.isEmpty()){
                        registerClientDevice(deviceRegIdForGCM);
                        //Toast.makeText(getApplicationContext(), "Device Registered : "+deviceRegIdForGCM, Toast.LENGTH_LONG).show();
                    }else{
                        // Toast.makeText(getApplicationContext(), "Device Registration Failed: "+deviceRegIdForGCM, Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    //Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_LONG).show();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                hideDialog();
            }
        }.execute(null, null, null);


    } //End of onCreate


    //When click on drawer item
    //Navigation drawer
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_client_dashboard) {
            // Handle the  action
            ClientDashboardFragment clientDashboardFragment =  new ClientDashboardFragment();
            getFragmentManager().beginTransaction().replace(R.id.flMain, clientDashboardFragment).addToBackStack(null).commit();
        } else if (id == R.id.nav_place_order){
            PlaceOrderFragment placeOrderFragment =  new PlaceOrderFragment();
            getFragmentManager().beginTransaction().replace(R.id.flMain, placeOrderFragment).addToBackStack(null).commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //Register Device for GCM  service
    public void registerClientDevice(String projectNumber){
        String MSG;

        // Tag used to cancel the request
        String tag_string_req = "req_login";

        String serverAddress = serverUrl+"/rest_controller/register_client_device/"+loggedInClient.getId()+"/"+deviceRegIdForGCM;

        Map<String, String> params = new HashMap<String, String>();

        JsonObjectRequest registerClientDeviceRequest = new JsonObjectRequest(Request.Method.POST,
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

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Login Error", "Login Error: " + error.getMessage());
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(registerClientDeviceRequest);

    }//End of registerRiderDevice

    //Show Diaslog
    private void showDialog() {
        pDialog.setMessage("Please Wait....");
        pDialog.setTitle("Proccessing");
        pDialog.show();
    }

    //Hide Dialog
    private void hideDialog() {
        pDialog.dismiss();
    }

}
