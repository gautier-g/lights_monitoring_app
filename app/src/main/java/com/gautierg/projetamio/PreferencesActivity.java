package com.gautierg.projetamio;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PreferencesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new PreferencesFragment())
            .commit();
    }
}