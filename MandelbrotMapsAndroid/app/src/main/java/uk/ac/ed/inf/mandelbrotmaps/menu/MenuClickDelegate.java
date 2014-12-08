package uk.ac.ed.inf.mandelbrotmaps.menu;

public interface MenuClickDelegate {
    public void onResetClicked();

    public void onSettingsClicked();

    public void onDetailClicked();

//    public void onSaveClicked();
//
//    public void onShareClicked();

    public void onHelpClicked();

    public void onSwitchRendererClicked();

    public void onSwapViewsClicked();

    public void onSwitchLayoutClicked();
}
