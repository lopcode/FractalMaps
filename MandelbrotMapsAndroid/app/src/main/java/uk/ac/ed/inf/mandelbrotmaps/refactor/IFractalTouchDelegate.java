package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalTouchDelegate {
    public void startDragging();

    public void stopDragging(boolean stoppedOnZoom);

    public void startScaling(float x, float y);

    public void stopScaling();

    public void dragFractal(float x, float y);

    public void scaleFractal(float scaleFactor, float midX, float midY);
}
