package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SettingsActivity extends ActionBarActivity {
    private static final String CRUDE_OPTION = "CRUDE";
    private static final boolean CRUDE_OPT_DEFAULT = true;

    private static final String SHOW_TIMES_OPTION = "SHOW_TIMES";
    private static final boolean SHOW_TIMES_OPT_DEFAULT = true;

    @InjectView(R.id.mainToolbar)
    Toolbar mainToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        ButterKnife.inject(this);

        setSupportActionBar(mainToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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