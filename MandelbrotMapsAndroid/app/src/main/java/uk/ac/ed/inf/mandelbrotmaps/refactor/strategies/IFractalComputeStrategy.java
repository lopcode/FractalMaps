package uk.ac.ed.inf.mandelbrotmaps.refactor.strategies;

import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeDelegate;

public interface IFractalComputeStrategy {
    public void initialise(int width, int height, IFractalComputeDelegate delegate);

    public void computeFractal(int pixelBlockSize,
                               final boolean showRenderingProgress,
                               final int maxIterations,
                               final int linesPerProgressUpdate,
                               final int defaultPixelSize,
                               final int xPixelMin,
                               final int xPixelMax,
                               final int yPixelMin,
                               final int yPixelMax,
                               final double xMin,
                               final double yMax,
                               final double pixelSize,
                               int[] pixelBuffer,
                               int[] pixelBufferSizes);

    public boolean shouldPerformCrudeFirst();

    public void setColourStrategy(IColourStrategy colourStrategy);

    public double getIterationBase();

    public double getIterationConstantFactor();
}
