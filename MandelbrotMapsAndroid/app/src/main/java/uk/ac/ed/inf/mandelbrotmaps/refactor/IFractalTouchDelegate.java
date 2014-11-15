package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalTouchDelegate {
    public void dragFractal(int x, int y);

    public void scaleFractal(float scaleFactor, float midX, float midY);
}
