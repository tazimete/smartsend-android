package com.example.smartsend.smartsendapp;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsend.smartsendapp.utilities.Client;
import com.example.smartsend.smartsendapp.utilities.Outlet;
import com.example.smartsend.smartsendapp.utilities.ServerManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pict-xx on 10/6/2016.
 */

public class PlaceOrderFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    private Button btnOrder;
    private Spinner spOutlet;
    private Context ctx;
    private ProgressDialog pDialog;
    private ConnectivityDetector connectivityDetector;
    private Outlet selectedOutlet;
    private EditText etPickupDateTime, etDeliverDateTime, etMobileNumber, etCustomerName, etPostalCode, etAddress, etUnitNumberFirst, etUnitNumberLast, etFoodCost, etReceiptNumber;
    private String pickupDateTime, deliverDateTime, mobileNumber, customerName, postalCode, address, unitNumberFirst, unitNumberLast, foodCost, receiptNumber;
    private String serverUrl;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         super.onCreateView(inflater, container, savedInstanceState);

        View placeOrderFragment = inflater.inflate(R.layout.layout_place_order_fragment, container, false);
        ctx = getActivity();
        serverUrl = ServerManager.getServerUrl(getActivity());

        //initilize objects
        btnOrder = (Button) placeOrderFragment.findViewById(R.id.btnOrder);
        spOutlet = (Spinner) placeOrderFragment.findViewById(R.id.spOutlet);

        etPickupDateTime = (EditText) placeOrderFragment.findViewById(R.id.etPickupDateAndTime);
        etDeliverDateTime = (EditText) placeOrderFragment.findViewById(R.id.etDeliverDateTime);
        etMobileNumber = (EditText) placeOrderFragment.findViewById(R.id.etMobileNumber);
        etCustomerName = (EditText) placeOrderFragment.findViewById(R.id.etCustomerName);
        etPostalCode = (EditText) placeOrderFragment.findViewById(R.id.etPostalCode);
        etAddress = (EditText) placeOrderFragment.findViewById(R.id.etAddress);
        etUnitNumberFirst = (EditText) placeOrderFragment.findViewById(R.id.etUnitNoFirst);
        etUnitNumberLast = (EditText) placeOrderFragment.findViewById(R.id.etUnitNumberLast);
        etFoodCost = (EditText) placeOrderFragment.findViewById(R.id.etFoodCost);
        etReceiptNumber = (EditText) placeOrderFragment.findViewById(R.id.etReceiptNumber);


        // Dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setCancelable(false);
        connectivityDetector = new ConnectivityDetector(ctx);

        UserLocalStore userLocalStore = new UserLocalStore(getActivity());
        final Client loggedInClient = userLocalStore.getLogedInClient();
        final int loggedInClientId = loggedInClient.getId();
        final int maxLengthOfPostCode = 6;

        //Loading outlets
        getOutletsbyClientId(loggedInClientId,  spOutlet);
        spOutlet.setOnItemSelectedListener(this);

        //Change button color when click
        btnOrder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getBackground().setAlpha(150);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getBackground().setAlpha(255);
                }
                return false;
            }
        });

        //Validation after btnOrder is clicked
        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Initialiaze the string
                pickupDateTime = etPickupDateTime.getText().toString().trim();
                deliverDateTime = etDeliverDateTime.getText().toString().trim();
                mobileNumber = etMobileNumber.getText().toString().trim();
                customerName = etCustomerName.getText().toString().trim();
                postalCode = etPostalCode.getText().toString().trim();
                address = etAddress.getText().toString().trim();
                unitNumberFirst = etUnitNumberFirst.getText().toString().trim();
                unitNumberLast = etUnitNumberFirst.getText().toString().trim();
                foodCost = etFoodCost.getText().toString().trim();
                receiptNumber = etReceiptNumber.getText().toString().trim();


                if(pickupDateTime.isEmpty() || deliverDateTime.isEmpty() || mobileNumber.isEmpty() || mobileNumber.isEmpty() || customerName.isEmpty() ||
                        postalCode.isEmpty() || address.isEmpty() || unitNumberFirst.isEmpty() || unitNumberLast.isEmpty() || foodCost.isEmpty()){

                    Toast.makeText(getActivity(), "Please fill-up  all fields", Toast.LENGTH_SHORT).show();

                }else{
                    sendOrderToRider(loggedInClient, selectedOutlet, pickupDateTime, deliverDateTime, mobileNumber, customerName, postalCode,
                            address, unitNumberFirst, unitNumberLast, foodCost, receiptNumber);
                    //sendOrderToRider();
                }
            }
        });

        //Get address by postcode
        etPostalCode.setFocusable(true);

        //Get address by postcode
        etPostalCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence pCode, int start, int before, int count) {
                postalCode = etPostalCode.getText().toString().trim();

                if( pCode.length() == maxLengthOfPostCode){
                    getAddressByPostalCode(postalCode);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Fteching Outlet of this client

        return placeOrderFragment;
    }


    //Get outlets by client id
    private void getOutletsbyClientId(final int id, final Spinner spOutlet) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";
        UserLocalStore userLocalStore = new UserLocalStore(ctx);
        Client loggedInClient = userLocalStore.getLogedInClient();
        final int loggedInClientId = loggedInClient.getId();
        final String loggedInClientName = loggedInClient.getCompanyName();

        String serverAddress = serverUrl+"/rest_controller/get_all_outlet_by_client_id/"+loggedInClientId;

        Toast.makeText(ctx,"Client ID : "+loggedInClientId, Toast.LENGTH_LONG).show();
        showDialog();

        Map<String, String> params = new HashMap<String, String>();

        JsonObjectRequest fetchOutletsRequest = new JsonObjectRequest(Request.Method.POST,
                serverAddress, new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {
                // Log.d("Login Response", "Login Response: " + response.toString());
                hideDialog();

                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){
                        int JSONLength = jObj.getInt("length");
                        ArrayList<Outlet> outlets = new ArrayList<Outlet>();
                        //ArrayList<String> outlets = new ArrayList<String>();
                        // outlets.add(0, new Outlet(loggedInClientId, loggedInClientName, 1));
                        Toast.makeText(ctx, "Json Length :" + JSONLength, Toast.LENGTH_LONG).show();

                        for (int i=0; i<JSONLength; i++) {
                            outlets.add(i, new Outlet(jObj.getInt("outlet_id_"+i), jObj.getString("outlet_" + i), jObj.getInt("outlet_type_"+i)));
                        }

                        //for (int i=0; i<JSONLength; i++) {
                        //     outlets.add(jObj.getString("outlet_" + i));
                        // }

                        //Binding outlets to spinner
                        ArrayAdapter outletAdapter = new ArrayAdapter(getActivity(), R.layout.layout_outlet_spinner, outlets);
                        spOutlet.setAdapter(outletAdapter);

                        String successMessage = jObj.getString("success_message");
                        Toast.makeText(ctx, "Success:" + successMessage, Toast.LENGTH_LONG).show();
                        // return outlets;
                    }else{
                        String errorMessage = jObj.getString("error_message");
                        Toast.makeText(ctx, "Error: "+errorMessage, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(ctx, "(RL) Json catch error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Login Error", "Login Error: " + error.getMessage());
                Toast.makeText(ctx,
                        " (RL) Error Response: "+ error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(fetchOutletsRequest);

    }

    //Get address by postal code and check distance between outlet and deliver address
    public void getAddressByPostalCode(String postalCode){
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        String serverAddress = serverUrl+"/rest_controller/get_address_by_postal_code/"+postalCode+"/"+selectedOutlet.getId()+"/"+selectedOutlet.getType();

       //dialog
        showDialog();

        Map<String, String> params = new HashMap<String, String>();

        JsonObjectRequest fetchAddressByPostalCodeRequest = new JsonObjectRequest(Request.Method.POST,
                serverAddress, new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {
                // Log.d("Login Response", "Login Response: " + response.toString());
                hideDialog();

                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){

                        String successMessage = jObj.getString("success_message");
                        Toast.makeText(ctx, "Success:" + successMessage, Toast.LENGTH_LONG).show();

                        String zip_bulding_no = jObj.getString("zip_bulding_no");
                        String zip_bulding_name = jObj.getString("zip_bulding_name");
                        String zip_street_name = jObj.getString("zip_street_name");
                        String zip_code = jObj.getString("zip_code");

                        String address = genrateAddress(zip_bulding_no, zip_bulding_name, zip_street_name, zip_code, "", "");

                        //Set gererated address to address field
                        etAddress.setText(address);

                        // return outlets;
                    }else{
                        String errorMessage = jObj.getString("error_message");
                        Toast.makeText(ctx, "Error Fetching Address: "+errorMessage, Toast.LENGTH_LONG).show();
                        connectivityDetector.showAlertDialog(ctx, "Invalid postcode", errorMessage);
                        etPostalCode.setText("");
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(ctx, "(FA) Json catch error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    etPostalCode.setText("");
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Login Error", "Login Error: " + error.getMessage());
                Toast.makeText(ctx,
                        " (FA) Error Response: "+ error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
                connectivityDetector.showAlertDialog(ctx, "Try Again", "Connection Failed");
                etPostalCode.setText("");
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(fetchAddressByPostalCodeRequest);
    }


    //Send order to rider
    //public void sendOrderToRider(){
    public void sendOrderToRider(Client loggedInClient, Outlet outlet, String npickupDateTime, String ndeliverDateTime, String nmobileNumber, String ncustomerName,
                                 String npostalCode, String naddress, String nunitNumberFirst, String nunitNumberLast, String nfoodCost, String nreceiptNumber){
        // Tag used to cancel the request
        String tag_string_req = "req_login";
        String serverAddress = serverUrl+"/rest_controller/send_order_to_rider";

        //dialog
        showDialog();

        Toast.makeText(ctx, "Client ID: "+loggedInClient.getId(), Toast.LENGTH_LONG).show();

        Map<String, String> params = new HashMap<String, String>();
        params.put("client_id", Integer.toString(loggedInClient.getId()));
        params.put("outlet_id", Integer.toString(outlet.getId()));
        params.put("outlet_name", outlet.getName());
        params.put("outlet_type", Integer.toString(outlet.getType()));
        params.put("pickup_datetime", npickupDateTime);
        params.put("deliver_datetime", ndeliverDateTime);
        params.put("mobile_number", nmobileNumber);
        params.put("customer_name", ncustomerName);
        params.put("postal_code", npostalCode);
        params.put("address", naddress);
        params.put("unit_number_first", nunitNumberFirst);
        params.put("unit_number_last", unitNumberLast);
        params.put("food_cost", nfoodCost);
        params.put("receipt_number", nreceiptNumber);

        //String jsonStringValue = params.toString();

        CustomRequest sendOrederToRiderRequest = new CustomRequest(Request.Method.POST,
                serverAddress, params , new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject jObj) {
                // Log.d("Login Response", "Login Response: " + response.toString());
                hideDialog();

                try {
                    boolean error = jObj.getBoolean("error");

                    if(!error){

                        String successMessage = jObj.getString("success_message");
                        String acceptedRiderName = jObj.getString("rider_name_0");
                        Toast.makeText(ctx, "Success:" + successMessage+"--"+acceptedRiderName, Toast.LENGTH_LONG).show();

                    }else{
                        String errorMessage = jObj.getString("error_message");
                        Toast.makeText(ctx, "Error Order: "+errorMessage, Toast.LENGTH_LONG).show();
                        connectivityDetector.showAlertDialog(ctx, "Order Empty", errorMessage);
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(ctx, "(SO) Json catch error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Login Error", "Login Error: " + error.getMessage());
                Toast.makeText(ctx,
                        " (SO) Error Response: "+ error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
                connectivityDetector.showAlertDialog(ctx, "Try Again", "Connection Failed");
            }
        });

        // Adding request to request queue
        //AppController.getInstance().addToRequestQueue(strReq);
        Volley.newRequestQueue(ctx).add(sendOrederToRiderRequest);
    }


    //Generate full address
    public String  genrateAddress(String zipBuldingNo, String zipBuldingName, String zipStreetName, String zipCode,
                                  String unitNoFirst, String unitNoLast){

        String address = zipBuldingNo+" ";
        String unitNumber = "";

        if(!zipBuldingName.isEmpty()){
            address = address + '('+zipBuldingName+')';
        }

        address = address + ", ";
        address = address + zipStreetName +", ";

        if(!unitNoFirst.isEmpty()){
            unitNumber = unitNoFirst;
        }

        if(!unitNoLast.isEmpty()){
            unitNumber = unitNumber+"-"+unitNoLast;
        }

        if(!unitNumber.isEmpty()){
            address = address + '#' + unitNumber +",  ";
        }

        address = address + "Singapore-" + zipCode;
        return address;
    }

    //Show Diaslog
    private void showDialog() {
        pDialog.setMessage("Please Wait....");
        pDialog.setTitle("Proccessing");
        pDialog.setCancelable(false);
        pDialog.show();
    }

    //Hide Dialog
    private void hideDialog() {
        pDialog.dismiss();
    }

    //Spinner Listener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedOutlet = (Outlet) parent.getItemAtPosition(position);
        Toast.makeText(ctx, "Selected Item Name : "+selectedOutlet.getName()+"\n Selected ID : "+selectedOutlet.getId(), Toast.LENGTH_LONG).show();

        etPostalCode.setText("");
        // String updatedPostCode = etPostalCode.getText().toString().trim();
        //if(updatedPostCode.length() == 6){
        //    getAddressByPostalCode(updatedPostCode);
        //}

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
