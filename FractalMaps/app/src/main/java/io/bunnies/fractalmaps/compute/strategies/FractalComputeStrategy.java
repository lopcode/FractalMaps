package io.bunnies.fractalmaps.compute.strategies;

import io.bunnies.fractalmaps.colouring.EnumColourStrategy;
import io.bunnies.fractalmaps.compute.IFractalComputeDelegate;

public abstract class FractalComputeStrategy implements IFractalComputeStrategy {
    protected int width;
    protected int height;
    protected IFractalComputeDelegate delegate;
    protected EnumColourStrategy colourStrategy;

    // Render calculating variables
    protected double xMin, yMax, pixelSize;

    @Override
    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        this.width = width;
        this.height = height;
        this.delegate = delegate;
    }

    @Override
    public void setColourStrategy(EnumColourStrategy colourStrategy) {
        this.colourStrategy = colourStrategy;
    }

    @Override
    public EnumColourStrategy getColourStrategy() {
        return this.colourStrategy;
    }
}
