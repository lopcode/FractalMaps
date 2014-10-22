package uk.ac.ed.inf.mandelbrotmaps.menu;

import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import uk.ac.ed.inf.mandelbrotmaps.R;

public class MenuDialog extends DialogFragment {
    public MenuDialog() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        View view = inflater.inflate(R.layout.menu_dialog, container);

        // Grab references to views here

        return view;
    }
}
