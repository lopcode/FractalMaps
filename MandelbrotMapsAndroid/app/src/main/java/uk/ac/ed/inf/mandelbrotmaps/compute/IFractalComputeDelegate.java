package uk.ac.ed.inf.mandelbrotmaps.compute;

public interface IFractalComputeDelegate {
    public void postUpdate(int[] pixels, int[] pixelSizes);

    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize, double timeTakenInSeconds);

    public void onComputeStarted(int pixelBlockSize);
}
