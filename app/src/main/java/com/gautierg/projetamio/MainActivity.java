package com.gautierg.projetamio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private List<DataEntry> data = new ArrayList<DataEntry>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Log.d("MainActivity", "Création de l'activité");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();

        TextView tv2 = findViewById(R.id.TV2);
        tv2.setText(PollingService.isRunning ? "en cours" : "arrêté");

        SwitchMaterial toggleButton = findViewById(R.id.material_switch);
        toggleButton.setChecked(PollingService.isRunning);

        MaterialCheckBox checkbox = findViewById(R.id.material_checkbox);
        checkbox.setChecked(preferences.getBoolean("boot_start", false));

        Button preferencesButton = findViewById(R.id.pref_button);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(MainActivity.this, PollingService.class));
                }
                else {
                    stopService(new Intent(MainActivity.this, PollingService.class));
                }
            }
        });

        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("MainActivity", "Boot start initiated");
                    editor.putBoolean("boot_start", true);
                }
                else {
                    Log.d("MainActivity", "Boot start stopped");
                    editor.putBoolean("boot_start", false);
                }
                editor.apply();
            }
        };

        checkbox.setOnCheckedChangeListener(listener);

        preferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
            }
        });

        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
                assert key != null;
                if (key.equals("boot_start")) {
                    checkbox.setOnCheckedChangeListener(null);
                    checkbox.setChecked((boolean) preferences.getAll().get("boot_start"));
                    checkbox.setOnCheckedChangeListener(listener);
                }
            }
        });

        ContextCompat.registerReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateServiceLabels();
            }
        }, new IntentFilter("UPDATE_SERVICE"), ContextCompat.RECEIVER_NOT_EXPORTED);

        ContextCompat.registerReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LinearLayout scrollzone = findViewById(R.id.scrollzone);
                scrollzone.setBackgroundColor(Color.parseColor("#001713"));

                long timestamp = intent.getLongExtra("timestamp", 0);
                boolean isPoweredOn = intent.getBooleanExtra("isPoweredOn", false);
                double value = intent.getDoubleExtra("value", 0);
                String mote = intent.getStringExtra("mote");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
                String dateFormatee = sdf.format(new Date(timestamp));

                DataEntry dataAlreadyExisting = null;

                for (DataEntry dataEntry : data) {
                    if (dataEntry.mote.equals(mote)) {
                        dataAlreadyExisting = dataEntry;
                    }
                }

                if (dataAlreadyExisting != null) {
                    LinearLayout panneau = new LinearLayout(getApplicationContext());
                    for (int i = 0; i < scrollzone.getChildCount(); i++) {
                        View child = scrollzone.getChildAt(i);
                        if (mote.equals(child.getTag())) {
                            panneau = (LinearLayout) child;
                            break;
                        }
                    }

                    TextView tvDate = (TextView) panneau.getChildAt(1);
                    TextView tvValue = (TextView) panneau.getChildAt(2);

                    tvDate.setText("Date : " + dateFormatee);
                    boolean newIsOn = false;

                    if ((!dataAlreadyExisting.isPoweredOn && ((value - dataAlreadyExisting.value) > 50)) || (dataAlreadyExisting.isPoweredOn && !((value - dataAlreadyExisting.value) < 50))) {
                        newIsOn = true;
                    }

                    tvValue.setText((newIsOn ? "Enabled" : "Disabled") + " : " + String.format(Locale.FRANCE, "%.2f", value));

                    dataAlreadyExisting.isPoweredOn = newIsOn;
                    dataAlreadyExisting.timestamp = timestamp;
                    dataAlreadyExisting.value = value;
                }
                else {
                    LinearLayout panneau = new LinearLayout(getApplicationContext());
                    panneau.setTag(mote);
                    panneau.setOrientation(LinearLayout.VERTICAL);
                    panneau.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    panneau.setBackgroundColor(Color.parseColor(isPoweredOn ? "#002921" : "#170000"));
                    panneau.setPadding(10, 10, 10, 10);

                    TextView tvDate = new TextView(getApplicationContext());
                    tvDate.setText("Date : " + dateFormatee);
                    tvDate.setTextSize(14);
                    tvDate.setTextColor(Color.WHITE);

                    TextView tvMote = new TextView(getApplicationContext());
                    tvMote.setText("Mote : " + mote);
                    tvMote.setTextSize(16);
                    tvMote.setTypeface(null, Typeface.BOLD);
                    tvMote.setTextColor(Color.WHITE);

                    TextView tvValue = new TextView(getApplicationContext());
                    tvValue.setText((isPoweredOn ? "Enabled" : "Disabled") + " : " + String.format(Locale.FRANCE, "%.2f", value));
                    tvValue.setTextSize(18);
                    tvValue.setTextColor(Color.WHITE);

                    panneau.addView(tvMote);
                    panneau.addView(tvDate);
                    panneau.addView(tvValue);

                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) panneau.getLayoutParams();
                    params.setMargins(10, 10, 10, 10);
                    panneau.setLayoutParams(params);

                    scrollzone.addView(panneau);

                    DataEntry dataEntry = new DataEntry();
                    dataEntry.timestamp = timestamp;
                    dataEntry.mote = mote;
                    dataEntry.isPoweredOn = isPoweredOn;
                    dataEntry.value = value;
                    data.add(dataEntry);


                }
            }
        }, new IntentFilter("RECEIVE_DATA"), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    protected void updateServiceLabels() {
        TextView tv2 = findViewById(R.id.TV2);
        SwitchMaterial materialSwitch = findViewById(R.id.material_switch);

        if (PollingService.isRunning) {
            tv2.setText("en cours");
            materialSwitch.setChecked(true);
        }
        else {
            tv2.setText("arrêté");
            materialSwitch.setChecked(false);
        }
    }

    public boolean estEnSemaineEntre19hEt23h(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            Date date = sdf.parse(timestamp);

            if (date == null) {
                return false;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            int jourDeLaSemaine = calendar.get(Calendar.DAY_OF_WEEK);
            boolean estEnSemaine = (jourDeLaSemaine >= Calendar.MONDAY && jourDeLaSemaine <= Calendar.FRIDAY);

            int heure = calendar.get(Calendar.HOUR_OF_DAY);
            boolean estEntre19hEt23h = (heure >= 19 && heure < 23);

            return estEnSemaine && estEntre19hEt23h;

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}