package com.huda.speedometer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_ID = 1;
    FusedLocationProviderClient mFusedLocationClient;
    private long firstTime = 0;
    private boolean from10to30 = false;
    private Thread thread;
    private float speed;
    private static final String TAG = "MainActivity";
    private TextView txtSpeed, speedInc, speedDec;
    private boolean stopThread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtSpeed = findViewById(R.id.speed_meter);
        speedInc = findViewById(R.id.calc_time);
        speedDec = findViewById(R.id.calc_time_dec);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
    }

    @Override
    protected void onResume() {

        super.onResume();
        stopThread = false;
        if (checkPermissions()) {
            getLastLocation();

            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        while (!stopThread) {
                            Thread.sleep(1000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getLastLocation();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                    }
                }
            };

            thread.start();
        }
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information

            }
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location != null) {
                                    requestNewLocationData();
                                    speed = location.getSpeed();
                                    String d = new DecimalFormat("##.##").format(speed * 3.6);
                                    float dFloat = Float.parseFloat(d);
                                    int speedInt = (int) dFloat;
                                    if (dFloat == 0) {
                                        txtSpeed.setText("0.0");
                                    } else {
                                        txtSpeed.setText(d);
                                    }
                                    Log.d(TAG, "onComplete: "+firstTime);
                                    Log.d(TAG, "onComplete: "+speedInt);
                                    if (firstTime == 0) {
                                        if (speedInt == 10) {

                                            firstTime = location.getTime();
                                            from10to30 = true;
                                            Log.d(TAG, "from10to30: "+from10to30);
                                        }

                                        if (speedInt == 30) {
                                            firstTime = location.getTime();
                                            from10to30 = false;
                                            Log.d(TAG, "from10to30: "+from10to30);
                                        }
                                    } else {
                                        if (from10to30) {
                                            if (speedInt == 30) {
                                                speedInc.setText(String.valueOf((location.getTime() - firstTime) / 1000));
                                                Log.d(TAG, "seconds: "+String.valueOf((location.getTime() - firstTime) / 1000));
                                            }
                                        } else {
                                            if (speedInt == 10) {
                                                speedDec.setText(String.valueOf((location.getTime() - firstTime) / 1000));
                                                Log.d(TAG, "seconds: "+String.valueOf((location.getTime() - firstTime) / 1000));

                                            }
                                        }
                                        firstTime = 0;
                                        Log.d(TAG, "onComplete: "+firstTime);
                                    }
                                    Log.d(TAG, "speed: " + new DecimalFormat("##.##").format(speed * 3.6));
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            speed=mLastLocation.getSpeed();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopThread = true;
    }
}
