package uk.ac.ed.inf.mandelbrotmaps.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import uk.ac.ed.inf.mandelbrotmaps.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
