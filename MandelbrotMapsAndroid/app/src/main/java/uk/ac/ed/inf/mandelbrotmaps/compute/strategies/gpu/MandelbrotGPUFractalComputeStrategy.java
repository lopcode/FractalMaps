package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

public class MandelbrotGPUFractalComputeStrategy extends GPUFractalComputeStrategy {
    @Override
    protected void invokeComputeFunction() {
        if (this.fractalRenderScript != null)
            this.fractalRenderScript.invoke_mandelbrot();
    }

    // Set the "maximum iteration" calculation constants
    // Empirically determined values for Mandelbrot set.
    public double getIterationBase() {
        return 1.24D;
    }

    public double getIterationConstantFactor() {
        return 54.0D;
    }

    public double getMaxZoomLevel() {
        return -31;
    }
}
