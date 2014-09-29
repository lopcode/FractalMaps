package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
    private static final String CRUDE_OPTION = "CRUDE";
    private static final boolean CRUDE_OPT_DEFAULT = true;

    private static final String SHOW_TIMES_OPTION = "SHOW_TIMES";
    private static final boolean SHOW_TIMES_OPT_DEFAULT = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    public static boolean performCrude(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(CRUDE_OPTION, CRUDE_OPT_DEFAULT);
    }

    public static boolean showTimes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SHOW_TIMES_OPTION, SHOW_TIMES_OPT_DEFAULT);
    }
}