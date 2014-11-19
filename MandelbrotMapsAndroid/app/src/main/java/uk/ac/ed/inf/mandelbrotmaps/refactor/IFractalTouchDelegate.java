package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalTouchDelegate {
    public void startDragging();

    public void stopDragging(boolean stoppedOnZoom, float totalDragX, float totalDragY);

    public void startScaling(float x, float y);

    public void stopScaling();

    public void dragFractal(float x, float y, float totalDragX, float totalDragY);

    public void scaleFractal(float scaleFactor, float midX, float midY);

    public void onLongClick(float x, float y);
}
