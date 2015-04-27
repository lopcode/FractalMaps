package io.bunnies.fractalmaps.presenter;

import java.util.List;

import io.bunnies.fractalmaps.overlay.IFractalOverlay;

public interface IFractalPresenterDelegate {
    public void onSceneOverlaysChanged(List<IFractalOverlay> overlays);
}
