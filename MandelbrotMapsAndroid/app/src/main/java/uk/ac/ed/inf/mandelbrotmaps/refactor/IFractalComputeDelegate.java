package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalComputeDelegate {
    public void postUpdate(int[] pixels, int[] pixelSizes);

    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize);
}
