package io.bunnies.fractalmaps.compute.strategies;

import io.bunnies.fractalmaps.colouring.EnumColourStrategy;
import io.bunnies.fractalmaps.compute.FractalComputeArguments;
import io.bunnies.fractalmaps.compute.IFractalComputeDelegate;

public interface IFractalComputeStrategy {
    public void initialise(int width, int height, IFractalComputeDelegate delegate);

    public void tearDown();

    public void computeFractal(FractalComputeArguments arguments);

    public boolean shouldPerformCrudeFirst();

    public void setColourStrategy(EnumColourStrategy colourStrategy);

    public EnumColourStrategy getColourStrategy();

    public double getIterationBase();

    public double getIterationConstantFactor();

    public double getMaxZoomLevel();

    public void stopAllRendering();
}
