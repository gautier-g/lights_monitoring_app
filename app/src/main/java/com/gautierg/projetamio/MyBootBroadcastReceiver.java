package com.gautierg.projetamio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.Objects;

public class MyBootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
            Log.d("MyBootBroadcastReceiver","Boot start broadcast received");

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean startAtBoot = preferences.getBoolean("boot_start", false);

            if (startAtBoot) {
                Intent serviceIntent = new Intent(context, PollingService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
