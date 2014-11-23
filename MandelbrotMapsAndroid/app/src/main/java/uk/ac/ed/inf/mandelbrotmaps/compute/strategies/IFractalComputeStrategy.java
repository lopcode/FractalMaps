package uk.ac.ed.inf.mandelbrotmaps.compute.strategies;

import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;

public interface IFractalComputeStrategy {
    public void initialise(int width, int height, IFractalComputeDelegate delegate);

    public void tearDown();

    public void computeFractal(FractalComputeArguments arguments);

    public boolean shouldPerformCrudeFirst();

    public void setColourStrategy(IColourStrategy colourStrategy);

    public double getIterationBase();

    public double getIterationConstantFactor();

    public void stopAllRendering();
}
