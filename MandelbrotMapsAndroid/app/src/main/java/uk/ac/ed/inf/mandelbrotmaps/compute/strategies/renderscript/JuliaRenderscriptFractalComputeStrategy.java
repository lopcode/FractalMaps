package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.renderscript;

import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.JuliaSeedSettable;

public class JuliaRenderscriptFractalComputeStrategy extends RenderscriptFractalComputeStrategy implements JuliaSeedSettable {
    private double juliaX = 0;
    private double juliaY = 0;

    @Override
    public double getIterationBase() {
        return 1.58D;
    }

    @Override
    public double getIterationConstantFactor() {
        return 6.46D;
    }

    public double getMaxZoomLevel() {
        return -20;
    }

    @Override
    public double[] getJuliaSeed() {
        return new double[]{this.juliaX, this.juliaY};
    }

    @Override
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
