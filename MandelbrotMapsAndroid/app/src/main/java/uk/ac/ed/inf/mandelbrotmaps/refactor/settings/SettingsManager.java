package uk.ac.ed.inf.mandelbrotmaps.refactor.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import uk.ac.ed.inf.mandelbrotmaps.R;

public class SettingsManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context context;

    public SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        PreferenceManager.setDefaultValues(this.context, R.xml.settings, false);
    }

    // Preference keys

    private static final String PREFERENCE_KEY_CRUDE_FIRST = "CRUDE";
    private static final boolean PREFERENCE_CRUDE_FIRST_DEFAULT = true;

    private static final String PREFERENCE_KEY_SHOW_TIMES = "SHOW_TIMES";
    private static final boolean PREFERENCE_SHOW_TIMES_DEFAULT = false;

    // Specific settings

    public boolean performCrudeFirst() {
        boolean result = PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean(PREFERENCE_KEY_CRUDE_FIRST, PREFERENCE_CRUDE_FIRST_DEFAULT);
        Log.i("SM", "Perform crude first: " + result);
        return result;
    }

    public boolean showTimes() {
        return PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean(PREFERENCE_KEY_CRUDE_FIRST, PREFERENCE_SHOW_TIMES_DEFAULT);
    }

    // OnSharedPreferenceChangeListener

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
