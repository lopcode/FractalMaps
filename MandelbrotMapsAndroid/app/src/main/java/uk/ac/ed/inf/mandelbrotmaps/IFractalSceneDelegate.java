package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.overlay.PinColour;

public interface IFractalSceneDelegate {
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering);

    public void onFractalLongClick(IFractalPresenter presenter, float x, float y);

    public void onFractalRecomputed(IFractalPresenter presenter);

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter);

    public void onPinColourChanged(PinColour colour);

    public void onMandelbrotColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender);

    public void onJuliaColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender);
}
