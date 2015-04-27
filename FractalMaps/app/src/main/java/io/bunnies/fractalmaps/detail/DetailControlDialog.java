package io.bunnies.fractalmaps.detail;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.bunnies.fractalmaps.R;
import io.bunnies.fractalmaps.settings.SettingsManager;

public class DetailControlDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private DetailControlDelegate delegate;

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

    public DetailControlDialog() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            delegate = (DetailControlDelegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DetailControlDelegate");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.detail_control_dialog, container);
        ButterKnife.inject(this, view);

        getDialog().setTitle(R.string.detail_title);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Get references to SeekBars, set their value from the prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mandelbrotBar.setOnSeekBarChangeListener(this);
        mandelbrotBar.setProgress((int) prefs.getFloat(SettingsManager.PREFERENCE_KEY_MANDELBROT_DETAIL, (float) SettingsManager.DEFAULT_DETAIL_LEVEL));

        juliaBar.setOnSeekBarChangeListener(this);
        juliaBar.setProgress((int) prefs.getFloat(SettingsManager.PREFERENCE_KEY_JULIA_DETAIL, (float) SettingsManager.DEFAULT_DETAIL_LEVEL));

        return view;
    }

    @OnClick(R.id.detail_cancel_button)
    public void onDetailCancelButtonClicked() {
        this.delegate.onCancelClicked();
    }

    @OnClick(R.id.default_detail_button)
    public void onDefaultDetailButtonClicked() {
        juliaBar.setProgress((int) SettingsManager.DEFAULT_DETAIL_LEVEL);
        mandelbrotBar.setProgress((int) SettingsManager.DEFAULT_DETAIL_LEVEL);
    }

    @OnClick(R.id.detail_apply_button)
    public void onDetailApplyButtonClicked() {
        //Set shared prefs and return value (to indicate if shared prefs have changed)
        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        prefsEditor.putFloat(SettingsManager.PREFERENCE_KEY_MANDELBROT_DETAIL, (float) mandelbrotBar.getProgress());
        prefsEditor.putFloat(SettingsManager.PREFERENCE_KEY_JULIA_DETAIL, (float) juliaBar.getProgress());

        prefsEditor.commit();
        this.delegate.onApplyChangesClicked();
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
