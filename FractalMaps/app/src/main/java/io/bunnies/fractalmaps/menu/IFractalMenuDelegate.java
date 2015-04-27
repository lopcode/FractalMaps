package io.bunnies.fractalmaps.menu;

import android.view.View;

public interface IFractalMenuDelegate {
    public void onResetFractalClicked(View viewContext);

    public void onSwitchFractalViewLayoutClicked(View viewContext);

    public void onChangeFractalPositionClicked(View viewContext);

    public void onSaveFractalClicked(View viewContext);

    public void onShareFractalClicked(View viewContext);
}
