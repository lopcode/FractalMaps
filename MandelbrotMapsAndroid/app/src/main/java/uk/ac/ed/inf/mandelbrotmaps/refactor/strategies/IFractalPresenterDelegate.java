package uk.ac.ed.inf.mandelbrotmaps.refactor.strategies;

import java.util.List;

import uk.ac.ed.inf.mandelbrotmaps.refactor.overlay.IFractalOverlay;

public interface IFractalPresenterDelegate {
    public void onSceneOverlaysChanged(List<IFractalOverlay> overlays);
}
