package com.example.wifiawarep2p;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyScanManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context context;

    private static final String[] LOCATION_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final String[] LOCATION_PERMS_COARSE={
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int INITIAL_REQUEST=1337;
    private static final int LOCATION_REQUEST_COARSE=INITIAL_REQUEST+2;
    private static final int LOCATION_REQUEST_FINE=INITIAL_REQUEST+3;

    // Hentet fra:https://github.com/anagramrice/NAN/blob/master/app/src/main/java/net/mobilewebprint/nan/MainActivity.java
    private static final int MY_PERMISSION_COARSE_LOCATION_REQUEST_CODE = 88;
    private static final int MY_PERMISSION_FINE_LOCATION_CODE = 99;
    //-------------------------------------------------------------------------------------------------------------------------


    private WifiAwareManager wifiAwareManager;
    private WifiAwareSession wifiAwareSession;

    private String LOG = "LOG-Test-Aware";

    /* TODO:
     *   1. implement wifi aware
     *     - check if wifi aware exists
     *     - check for permissions
     *     - check if there are someone in the cluster
     *     - retrieve the name of the others in the cluster?? - how should this be done? do we know who we want to talk to?
     *   2. add new chats in list and save locally on phone
     * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        addChatsToList();
        wifiAwareManager = (WifiAwareManager) getSystemService(Context.WIFI_AWARE_SERVICE);


        // Hentet fra:https://github.com/anagramrice/NAN/blob/master/app/src/main/java/net/mobilewebprint/nan/MainActivity.java
        setupPermissions();
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(LOCATION_PERMS, LOCATION_REQUEST_FINE);
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestPermissions(LOCATION_PERMS_COARSE, LOCATION_REQUEST_COARSE);
        }
        attachToSession();

    }

    private void addChatsToList(){
        ListView listView = findViewById(R.id.lvTest);

        // Construct the data source
        ArrayList<Chat> arrayOfUsers = new ArrayList<Chat>();

        UsersAdapter adapter = new UsersAdapter(this, arrayOfUsers);
        listView.setAdapter(adapter);
        //adapter.getItem(0);
        Chat newChat = new Chat("User1");
        adapter.add(newChat);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //TODO: onclick
            }
        });

        listView.setAdapter(adapter);
    }

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    }

    /**
     * App Permissions for Coarse Location
     **/
    private void setupPermissions(){
        // If we don't have the record network permission...
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // And if we're on SDK M or later...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ask again, nicely, for the permissions.
                String[] permissionsWeNeed = new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION };
                requestPermissions(permissionsWeNeed, MY_PERMISSION_COARSE_LOCATION_REQUEST_CODE);
            }
        }

        //-------------------------------------------------------------------------------------------- +++++
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // And if we're on SDK M or later...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ask again, nicely, for the permissions.
                String[] permissionsWeNeed = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
                requestPermissions(permissionsWeNeed, MY_PERMISSION_FINE_LOCATION_CODE);
            }
        }
        //-------------------------------------------------------------------------------------------- -----
    }

    private void attachToSession(){
        //Obtain a session, by calling attach(). Joins or forms a WifI aware cluster.
        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                super.onAttached(session);
                wifiAwareSession = session;

                Log.i(LOG, "ON Attached!");
                //TODO: close session when no longer needed
            }
            //TODO: make onattachfailed
        }, new IdentityChangedListener() {
            @Override
            //Sets the mac address for the device -> The implication is that peers you've been communicating with may no longer recognize you and you need to re-establish your identity - e.g. by starting a discovery session. TODO: make this work
            // TODO: should this be handled ^^
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                //setMacAddress(mac);
            }
        }, null);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_COARSE_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("LOG", "Permission for location granted.");
                    return;

                } else {
                    Log.i("LOG", "Permission for location not granted. NAN can't run.");
                    finish();
                    // The permission was denied, so we can show a message why we can't run the app
                    // and then close the app.
                }
            }

            //-------------------------------------------------------------------------------------------- +++++
            case MY_PERMISSION_FINE_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("LOG", "Fine location permission granted");
                    return;

                } else {
                    Log.i("LOG", "Fine location permission not granted");
                }
            }
            //-------------------------------------------------------------------------------------------- -----
            // Other permissions could go down here

        }
    }

}