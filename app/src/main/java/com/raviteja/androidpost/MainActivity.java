package com.raviteja.androidpost;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    TextView resultView;
    LocationManager manager;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    double last_lat, last_lon;
    final int MY_PERMISSIONS_REQUEST_LOCATION_FINE = 5;
    final int MY_PERMISSIONS_REQUEST_LOCATION_COARSE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION_FINE);
        }
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION_COARSE);
        }*/

        resultView = (TextView) findViewById(R.id.textView);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent iSettings = new Intent(this, SettingsActivity.class);
            this.startActivity(iSettings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendData(View view)
    {
        resultView.setText("");
        if (this.manager != null ) {
            this.manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        }
        sendToServer();
    }

    public void displayToast(String message)
    {
        Toast.makeText(this.getApplicationContext(),message, Toast.LENGTH_LONG).show();
    }

    public void sendToServer() {

        String lat, lon;
        if(this.last_lat == 0 && this.last_lon == 0) {
            lat = getString(R.string.def_lat);
            lon = getString(R.string.def_lon);
        }
        // get last known location
        else {
            lat = this.last_lat + "";
            lon = this.last_lon + "";
        }
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_STRING,0);
        String POST_URL = prefs.getString(SettingsActivity.SERVER_URL,null);

        try
        {
            if(POST_URL == null) {
                displayToast("ERROR: Please set server address in settings");
                return;
            }
            else if(!POST_URL.startsWith("http://")) {
                POST_URL = "http://" + POST_URL;
            }
            URL url = new URL(POST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("lat",lat)
                    .appendQueryParameter("lon",lon);
            String query = builder.build().getEncodedQuery();
            Log.d("query", query);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String line;
            while((line = reader.readLine())!=null) {
                resultView.append(line);
            }
            reader.close();
            conn.connect();
        }
        catch(IOException ioe)
        {
            Toast.makeText(this,ioe.toString(),Toast.LENGTH_LONG).show();
        }
    }
    public void clear(View view)
    {
        resultView.setText("");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("yo","Location Changed : "+getLocationString(location.getLatitude(),location.getLongitude()));
        this.last_lat = location.getLatitude();
        this.last_lon = location.getLongitude();

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d("yo", "Location Status Changed : " + s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d("yo","Location provider enabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d("yo","Location provider disabled");
    }

    public String getLocationString(double lat,double lon) {
        return "( "+lat+" , "+lon+" )";
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION_FINE:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    manager = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                }
                else
                {
                    displayToast("Sorry, App cannot work without this permission!");
                    System.exit(0);
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_LOCATION_COARSE:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (mGoogleApiClient == null) {
                        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .build();
                    }
                }
                else
                {
                    displayToast("Sorry, App cannot work without location permission!");
                    System.exit(0);
                }
                return;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("yo","Connected to play services");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            this.last_lat = mLastLocation.getLatitude();
            this.last_lon = mLastLocation.getLongitude();
            Log.d("yo",this.getLocationString(this.last_lat,this.last_lon));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("yo","Connected suspended from play services");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("yo","Connected to play services failed");
    }

    protected void onStart() {
        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        if(mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        super.onStop();
    }
}
