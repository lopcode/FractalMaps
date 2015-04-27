package io.bunnies.fractalmaps.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import io.bunnies.fractalmaps.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
