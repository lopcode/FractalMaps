package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

public class MandelbrotGPUFractalComputeStrategy extends GPUFractalComputeStrategy {
    @Override
    protected void invokeComputeFunction() {
        this.fractalRenderScript.invoke_mandelbrot();
    }
}
