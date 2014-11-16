package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalTouchDelegate {
    public void startDragging();

    public void stopDragging();

    public void dragFractal(float x, float y);

    public void scaleFractal(float scaleFactor, float midX, float midY);
}
