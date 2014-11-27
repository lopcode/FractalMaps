package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinColour;
import uk.ac.ed.inf.mandelbrotmaps.presenter.IFractalPresenter;

public interface IFractalSceneDelegate {
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering);

    public void onFractalLongClick(IFractalPresenter presenter, float x, float y);

    public void onFractalRecomputeScheduled(IFractalPresenter presenter);

    public void onFractalRecomputed(IFractalPresenter presenter, double timeInSeconds);

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter);

    public void onPinColourChanged(PinColour colour);

    public void onMandelbrotColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender);

    public void onJuliaColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender);

    public void onFractalViewReady(IFractalPresenter presenter);

    public void onSceneLayoutChanged(SceneLayoutEnum layoutType);
}
