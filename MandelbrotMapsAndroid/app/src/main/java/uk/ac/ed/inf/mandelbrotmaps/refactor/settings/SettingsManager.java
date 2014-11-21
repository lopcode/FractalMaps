package uk.ac.ed.inf.mandelbrotmaps.refactor.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import uk.ac.ed.inf.mandelbrotmaps.R;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalSceneDelegate;
import uk.ac.ed.inf.mandelbrotmaps.refactor.overlay.PinColour;

public class SettingsManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context context;
    private IFractalSceneDelegate sceneDelegate;

    public SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        PreferenceManager.setDefaultValues(this.context, R.xml.settings, false);
    }

    public void setFractalSceneDelegate(IFractalSceneDelegate sceneDelegate) {
        this.sceneDelegate = sceneDelegate;
    }

    // Preference keys

    private static final String PREFERENCE_KEY_CRUDE_FIRST = "CRUDE";
    private static final boolean PREFERENCE_CRUDE_FIRST_DEFAULT = true;

    private static final String PREFERENCE_KEY_SHOW_TIMES = "SHOW_TIMES";
    private static final boolean PREFERENCE_SHOW_TIMES_DEFAULT = false;

    private static final String PREFERENCE_KEY_PIN_COLOUR = "PIN_COLOUR";
    private static final String PREFERENCE_PIN_COLOUR_DEFAULT = "blue";

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this.context.getApplicationContext());
    }
    // Specific settings

    public boolean performCrudeFirst() {
        boolean result = this.getDefaultSharedPreferences().getBoolean(PREFERENCE_KEY_CRUDE_FIRST, PREFERENCE_CRUDE_FIRST_DEFAULT);
        Log.i("SM", "Perform crude first: " + result);
        return result;
    }

    public boolean showTimes() {
        return this.getDefaultSharedPreferences().getBoolean(PREFERENCE_KEY_CRUDE_FIRST, PREFERENCE_SHOW_TIMES_DEFAULT);
    }

    public void refreshPinSettings() {
        this.onSharedPreferenceChanged(this.getDefaultSharedPreferences(), PREFERENCE_KEY_PIN_COLOUR);
    }

    // OnSharedPreferenceChangeListener

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(PREFERENCE_KEY_PIN_COLOUR)) {
            PinColour pinColour;
            try {
                pinColour = PinColour.valueOf(sharedPreferences.getString(key, PREFERENCE_PIN_COLOUR_DEFAULT).toUpperCase());
            } catch (IllegalArgumentException e) {
                pinColour = PinColour.valueOf(PREFERENCE_PIN_COLOUR_DEFAULT);
            }

            this.sceneDelegate.onPinColourChanged(pinColour);
        }
    }
}
