package uk.ac.ed.inf.mandelbrotmaps.menu;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.ac.ed.inf.mandelbrotmaps.R;

public class MenuDialog extends DialogFragment {
    private MenuClickDelegate delegate;

    public MenuDialog() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            delegate = (MenuClickDelegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MenuClickDelegate");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View view = inflater.inflate(R.layout.menu_dialog, container);
        ButterKnife.inject(this, view);

        return view;
    }

    @OnClick(R.id.menuButtonReset)
    public void resetButtonClicked() {
        this.delegate.onResetClicked();
    }

    @OnClick(R.id.menuButtonToggleSmall)
    public void toggleSmallButtonClicked() {
        this.delegate.onToggleSmallClicked();
    }

    @OnClick(R.id.menuButtonTanLei)
    public void theoremButtonClicked () { this.delegate.onTheoremClicked(); }

    @OnClick(R.id.menuButtonSettings)
    public void settingsButtonClicked() {
        this.delegate.onSettingsClicked();
    }

    @OnClick(R.id.menuButtonDetail)
    public void detailButtonClicked() {
        this.delegate.onDetailClicked();
    }

    @OnClick(R.id.menuButtonSave)
    public void saveButtonClicked() {
        this.delegate.onSaveClicked();
    }

    @OnClick(R.id.menuButtonShare)
    public void shareButtonClicked() {
        this.delegate.onShareClicked();
    }

    @OnClick(R.id.menuButtonHelp)
    public void helpButtonClicked() {
        this.delegate.onHelpClicked();
    }
}
