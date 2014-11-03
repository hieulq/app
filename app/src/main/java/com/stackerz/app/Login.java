package com.stackerz.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by ed on 17/10/2014.
 */
public class Login extends Activity implements View.OnClickListener{

    public Button connect;
    public String username, password, tenant, endpoint;
    public EditText userInput, passInput, tenantInput, serverInput;
    public SharedPreferences shPref ;
    public Editor toEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SSLCerts.sslHandling();
        setContentView(R.layout.activity_login);
        getInit();
    }

    public void getInit(){
        connect = (Button)findViewById(R.id.connectButtonLogin);
        userInput = (EditText)findViewById(R.id.userName);
        serverInput = (EditText)findViewById(R.id.server);
        passInput = (EditText)findViewById(R.id.password);
        tenantInput = (EditText)findViewById(R.id.tenant);
        connect.setOnClickListener(this);

    }

    public void setSharedPrefs() {
        //shPref = getSharedPreferences("Login_Credentials", MODE_PRIVATE);
        shPref = new ObscuredSharedPreferences(this, this.getSharedPreferences("Login_Credentials", Context.MODE_PRIVATE) );
        toEdit = shPref.edit();
        toEdit.putString("Username", username);
        toEdit.putString("Password", password);
        toEdit.putString("Endpoint", endpoint);
        toEdit.putString("Tenant", tenant);
        toEdit.commit();

    }

    //public SharedPreferences getShPref() {
    //    return shPref;
    //}

    public void getSharedPrefs(){
        String storedUser = "", storedPass = "", storedURL ="", storedTenant="";
        shPref.getString("Username",storedUser);
        shPref.getString("Password",storedPass);
        shPref.getString("Endpoint",storedURL);
        shPref.getString("Tenant",storedTenant);
    }

    @Override
    public void onClick(View v){
        boolean reachable = false;
        v.getId();
        //v.setBackground(R.drawable.rounded_red);
        //v.setBackgroundColor(Color.RED);
        username = userInput.getText().toString();
        password = passInput.getText().toString();
        endpoint = serverInput.getText().toString();
        tenant = tenantInput.getText().toString();
        if (isNetworkAvailable()){
            if (isValidUrl(endpoint)){
                reachable = true;
            }else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        "This is not a valid URL address, make sure there are no blank spaces at the end. Please try again.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL,0,120);
                toast.show();
                //serverInput.setText("");
                reachable = false;
            }
        }else{
            Toast toast = Toast.makeText(getApplicationContext(),
                    "You don't seem to be connected to the network now. Please try again later.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL,0,120);
            toast.show();
            //serverInput.setText("");
            reachable = false;
        }
        if (username.isEmpty()||password.isEmpty()||tenant.isEmpty()){
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Keystone needs to know who you are. Check your user name, tenant and password.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL,0,120);
            toast.show();
            //serverInput.setText("");
            reachable = false;
        }
        if (reachable) {
            setSharedPrefs();
            loginRequest();
            //Intent intent = new Intent(Login.this, Stackerz.class);
            //startActivity(intent);
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private boolean isValidUrl(String url) {
        Pattern p = Patterns.WEB_URL;
        Matcher m = p.matcher(url);
        if(m.matches())
            return true;
        else
            return false;
    }

    public void loginRequest(){
        final String user = shPref.getString("Username",username);
        final String pass = shPref.getString("Password",password);
        final String url = shPref.getString("Endpoint", endpoint);
        final String tnt = shPref.getString("Tenant", tenant);
        String json = "{\"auth\": {\"tenantName\": \""+tnt+"\", \"passwordCredentials\": {\"username\": \""+user+"\", \"password\": \""+pass+"\"}}}";

        JSONObject auth = null;
        try {
            auth = new JSONObject(json);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.POST, url, auth,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        JSONObject access = null;
                        try {
                            access = response.getJSONObject("access");
                            JSONObject token = access.getJSONObject("token");
                            String id = token.getString("id");
                            Toast.makeText(getApplicationContext(), id, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }

                        Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        ){
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tenantName", tnt);
                params.put("username", user);
                params.put("password", pass);
                return params;
            }
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Vary", "X-Auth-Token");
                params.put("User-Agent", "stackerz");
                params.put("Accept", "application/json");
                params.put("Content-Type", "application/json; charset=utf-8");
                return params;
            }

        };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(getRequest);
        }
    }





