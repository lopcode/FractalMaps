package uk.ac.ed.inf.mandelbrotmaps.touch;

public interface IFractalTouchDelegate {
    public void startDraggingFractal();

    public void stopDraggingFractal(boolean stoppedOnZoom, float totalDragX, float totalDragY);

    public void startScalingFractal(float x, float y);

    public void stopScalingFractal();

    public void dragFractal(float x, float y, float totalDragX, float totalDragY);

    public void scaleFractal(float scaleFactor, float midX, float midY);

    public void onLongClick(float x, float y);
}
