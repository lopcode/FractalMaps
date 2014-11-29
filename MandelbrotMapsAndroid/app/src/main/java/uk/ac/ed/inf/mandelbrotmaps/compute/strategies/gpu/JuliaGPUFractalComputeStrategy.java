package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

public class JuliaGPUFractalComputeStrategy extends GPUFractalComputeStrategy {
    private double juliaX = 0;
    private double juliaY = 0;

    public double[] getJuliaSeed() {
        return new double[]{this.juliaX, this.juliaY};
    }

    public void setJuliaSeed(double juliaX, double juliaY) {
        this.juliaX = juliaX;
        this.juliaY = juliaY;
    }

    @Override
    protected void invokeComputeFunction() {
        this.fractalRenderScript.set_juliaX(this.juliaX);
        this.fractalRenderScript.set_juliaY(this.juliaY);
        this.fractalRenderScript.invoke_julia();
    }
}
