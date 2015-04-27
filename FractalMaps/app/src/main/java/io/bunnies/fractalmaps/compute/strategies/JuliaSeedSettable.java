package io.bunnies.fractalmaps.compute.strategies;

public interface JuliaSeedSettable {
    public double[] getJuliaSeed();

    public void setJuliaSeed(double juliaX, double juliaY);
}
