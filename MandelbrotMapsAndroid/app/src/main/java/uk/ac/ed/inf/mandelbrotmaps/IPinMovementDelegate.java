package uk.ac.ed.inf.mandelbrotmaps;

public interface IPinMovementDelegate {
    public void pinDragged(float x, float y, boolean forceUpdate);

    public float getPinX();

    public float getPinY();

    public float getPinRadius();

    public void startedDraggingPin();

    public void stoppedDraggingPin(float x, float y);
}
