package com.Madhav.solaire;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    ImageButton transcribeButton;
    ImageButton translateButton;
    ImageButton chatButton;
    ImageButton recipeButton;
    ImageButton bluetoothButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transcribeButton = findViewById(R.id.transcribeButton);
        translateButton = findViewById(R.id.translateButton);
        chatButton = findViewById(R.id.chatButton);
        recipeButton = findViewById(R.id.recipeButton);
        bluetoothButton = findViewById(R.id.bluetoothButton);

        transcribeButton.setOnClickListener((v)->{
            Intent intent = new Intent(MainActivity.this, TranscribeActivity.class);
            startActivity(intent);
        });

        translateButton.setOnClickListener((v)->{
            Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
            startActivity(intent);
        });

        chatButton.setOnClickListener((v)->{
            Intent intent = new Intent(MainActivity.this, chatActivity.class);
            startActivity(intent);
        });

        recipeButton.setOnClickListener((v)->{
            Intent intent = new Intent(MainActivity.this, recipeActivity.class);
            startActivity(intent);

        });

        bluetoothButton.setOnClickListener((v)->{
            if (BLEHelper.getInstance(this).getScanning()) {
                BLEHelper.getInstance(this).stopBleScan();
                try {
                    Thread.sleep(100);
                } catch (Exception e) { }
            }
            BLEHelper.getInstance(this).startBleScan();
        });

        final Observer<String> readDataObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String batteryStatus) {
                // perform action
                // below is example that worked for Google
                if (batteryStatus.startsWith("!TGB")){
                    System.out.println("Battery Update" + batteryStatus.substring(4));
                    //batteryText.setText("Battery: " + batteryStatus.substring(4) + "%");
                }

            }
        };

// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        BLEHelper.getInstance(this).getReadData().observe(this, readDataObserver);

// Create the observer which updates the UI.
        final Observer<Boolean> isConnectedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean connectionStatus) {
                // perform action
                // example that worked for Google
                System.out.println("Connection status: " + connectionStatus.toString());
                switch (connectionStatus.toString()) {
                    case "true":
                        //connectionText.setText("Bluetooth: Connected");
                        break;
                    case "false":
                        //connectionText.setText("Bluetooth: Disconnected");
                        break;
                }
            }
        };

// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        BLEHelper.getInstance(this).getIsConnected().observe(this, isConnectedObserver);
    }

    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        BLEHelper.getInstance(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}