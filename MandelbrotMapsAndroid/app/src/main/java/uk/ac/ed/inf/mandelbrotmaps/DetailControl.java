package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DetailControl extends Activity implements OnSeekBarChangeListener {
    @InjectView(R.id.detail_apply_button)
    Button applyButton;

    @InjectView(R.id.default_detail_button)
    Button defaultsButton;

    @InjectView(R.id.detail_cancel_button)
    Button cancelButton;

    @InjectView(R.id.mandelbrot_seekbar)
    SeekBar mandelbrotBar;

    @InjectView(R.id.julia_seekbar)
    SeekBar juliaBar;

    @InjectView(R.id.mandelbrotText)
    TextView mandelbrotText;

    @InjectView(R.id.juliaText)
    TextView juliaText;

    boolean changed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailcontrol);
        ButterKnife.inject(this);

        // Get references to SeekBars, set their value from the prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mandelbrotBar.setOnSeekBarChangeListener(this);
        mandelbrotBar.setProgress((int) prefs.getFloat(FractalActivity.mandelbrotDetailKey, (float) AbstractFractalView.DEFAULT_DETAIL_LEVEL));

        juliaBar.setOnSeekBarChangeListener(this);
        juliaBar.setProgress((int) prefs.getFloat(FractalActivity.juliaDetailKey, (float) AbstractFractalView.DEFAULT_DETAIL_LEVEL));
    }

    @OnClick(R.id.detail_cancel_button)
    public void onDetailCancelButtonClicked() {
        this.finish();
    }

    @OnClick(R.id.default_detail_button)
    public void onDefaultDetailButtonClicked() {
        juliaBar.setProgress((int) AbstractFractalView.DEFAULT_DETAIL_LEVEL);
        mandelbrotBar.setProgress((int) AbstractFractalView.DEFAULT_DETAIL_LEVEL);
    }

    @OnClick(R.id.detail_apply_button)
    public void onDetailApplyButtonClicked() {
        //Set shared prefs and return value (to indicate if shared prefs have changed)
        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        prefsEditor.putFloat(FractalActivity.mandelbrotDetailKey, (float) mandelbrotBar.getProgress());
        prefsEditor.putFloat(FractalActivity.juliaDetailKey, (float) juliaBar.getProgress());

        prefsEditor.commit();

        changed = true;

        finish();
    }

    @Override
    public void finish() {
        Intent result = new Intent();
        result.putExtra(FractalActivity.DETAIL_CHANGED_KEY, changed);

        setResult(Activity.RESULT_OK, result);

        super.finish();
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (seekBar.getId() == R.id.mandelbrot_seekbar) {
            mandelbrotText.setText(Integer.toString(seekBar.getProgress()));
        } else if (seekBar.getId() == R.id.julia_seekbar) {
            juliaText.setText(Integer.toString(seekBar.getProgress()));
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
 