package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalComputeStrategy {
    public void initialise(int width, int height, IFractalComputeDelegate delegate);

    public void computeFractal(int pixelBlockSize,
                               final boolean showRenderingProgress,
                               final int linesPerProgressUpdate,
                               final int xPixelMin,
                               final int xPixelMax,
                               final int yPixelMin,
                               final int yPixelMax,
                               final double xMin,
                               final double yMax,
                               final double pixelSize);

    public int[] getFractalResult();

    public int[] getPixelSizes();

    public void clearPixelSizes();

    public void translateFractal(int x, int y);
}
