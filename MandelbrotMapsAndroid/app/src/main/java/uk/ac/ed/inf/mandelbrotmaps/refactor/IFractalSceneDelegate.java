package uk.ac.ed.inf.mandelbrotmaps.refactor;

import uk.ac.ed.inf.mandelbrotmaps.refactor.overlay.PinColour;

public interface IFractalSceneDelegate {
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering);

    public void onFractalLongClick(IFractalPresenter presenter, float x, float y);

    public void onFractalRecomputed(IFractalPresenter presenter);

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter);

    public void onPinColourChanged(PinColour colour);
}
