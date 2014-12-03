package uk.ac.ed.inf.mandelbrotmaps.compute.strategies;

public interface JuliaSeedSettable {
    public double[] getJuliaSeed();

    public void setJuliaSeed(double juliaX, double juliaY);
}
