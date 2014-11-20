package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IPinMovementDelegate {
    public void pinDragged(float x, float y);

    public float getPinX();

    public float getPinY();

    public float getPinRadius();

    public void startedDraggingPin();

    public void stoppedDraggingPin();
}
