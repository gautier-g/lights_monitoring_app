package com.gautierg.projetamio;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.util.JsonReader;
import android.widget.Toast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class PollingService extends Service {
    public static boolean isRunning = false;
    private static final String TAG = "PollingService";
    private Timer pollThread = new Timer();
    Integer counter = 60;

    public PollingService() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (counter == 0 | counter == 60) {
                    repetedTask();
                }
                Log.d(TAG, String.valueOf(counter));
                counter = ((counter + 59) % 60);
            }
        };

        pollThread.schedule(timerTask, 0, 1000);
        Log.d(TAG, "Timer démarré - polling toutes les 60 secondes");
    }

    protected void repetedTask() {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL("http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last");

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }

                String responseString = response.toString();
                List<DataEntry> tmpList = parseJson(responseString);
                List<DataEntry> tmpListWithoutDuplicates = parseJson(responseString);

                for (DataEntry baseItem : tmpList) {
                    boolean isFound = false;

                    for (DataEntry obtainedItem : tmpListWithoutDuplicates) {
                        if ((baseItem.timestamp == obtainedItem.timestamp) ||
                                (baseItem.isPoweredOn == obtainedItem.isPoweredOn) ||
                                (baseItem.value == obtainedItem.value) ||
                                (Objects.equals(baseItem.mote, obtainedItem.mote))) {
                            isFound = true;
                        }
                    }
                    if (!isFound) {
                        tmpListWithoutDuplicates.add(baseItem);
                    }
                }

                for (DataEntry obtainedItem : tmpListWithoutDuplicates) {
                    Intent dataIntent = new Intent("RECEIVE_DATA");

                    dataIntent.putExtra("timestamp", obtainedItem.timestamp);
                    dataIntent.putExtra("isPoweredOn", obtainedItem.isPoweredOn);
                    dataIntent.putExtra("value", obtainedItem.value);
                    dataIntent.putExtra("mote", obtainedItem.mote);

                    dataIntent.setPackage("com.gautierg.projetamio");
                    getApplicationContext().sendBroadcast(dataIntent);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Une erreur est survenue lors de la connexion au serveur. (" + responseCode + ")", Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la requête HTTP", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erreur fermeture reader", e);
                }
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public List<DataEntry> parseJson(String jsonString) throws IOException {
        List<DataEntry> dataList = new ArrayList<>();
        JsonReader reader = new JsonReader(new StringReader(jsonString));
        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();

            if (name.equals("data")) {
                reader.beginArray();

                while (reader.hasNext()) {
                    DataEntry entry = new DataEntry();

                    reader.beginObject();

                    while (reader.hasNext()) {
                        String fieldName = reader.nextName();

                        switch (fieldName) {
                            case "timestamp":
                                entry.timestamp = reader.nextLong();
                                break;
                            case "value":
                                entry.value = reader.nextDouble();
                                break;
                            case "mote":
                                entry.mote = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }

                    entry.isPoweredOn = (entry.value >= 250);

                    reader.endObject();
                    dataList.add(entry);
                }

                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        reader.close();

        return dataList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pollThread.cancel();
        isRunning = false;
        Intent updateIntent = new Intent("UPDATE_SERVICE");
        updateIntent.setPackage("com.gautierg.projetamio");
        getApplicationContext().sendBroadcast(updateIntent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        Intent updateIntent = new Intent("UPDATE_SERVICE");
        updateIntent.setPackage("com.gautierg.projetamio");
        getApplicationContext().sendBroadcast(updateIntent);
        return START_STICKY;
    }
}