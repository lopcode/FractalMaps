package uk.ac.ed.inf.mandelbrotmaps;

import java.util.List;

import uk.ac.ed.inf.mandelbrotmaps.overlay.IFractalOverlay;

public interface IFractalPresenterDelegate {
    public void onSceneOverlaysChanged(List<IFractalOverlay> overlays);
}
