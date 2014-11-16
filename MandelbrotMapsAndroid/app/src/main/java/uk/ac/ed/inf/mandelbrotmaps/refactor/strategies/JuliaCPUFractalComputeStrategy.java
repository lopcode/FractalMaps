package uk.ac.ed.inf.mandelbrotmaps.refactor.strategies;

import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeDelegate;

public class JuliaCPUFractalComputeStrategy extends CPUFractalComputeStrategy {
    // Set the "maximum iteration" calculation constants
    // Empirically determined values for Julia sets.
    public double getIterationBase() {
        return 1.58D;
    }

    public double getIterationConstantFactor() {
        return 6.46D;
    }

    @Override
    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        super.initialise(width, height, delegate);

//        Set home area
//        homeGraphArea = new MandelbrotJuliaLocation().getJuliaGraphArea();
    }

    private double juliaX = 0;
    private double juliaY = 0;

    public void setJuliaSeed(double juliaX, double juliaY) {
        this.juliaX = juliaX;
        this.juliaY = juliaY;
    }

    @Override
    int pixelInSet(int xPixel, int yPixel, int maxIterations) {
        boolean inside = true;
        int iterationNr;
        double newx, newy;
        double x, y;

        x = xMin + ((double) xPixel * pixelSize);
        y = yMax - ((double) yPixel * pixelSize);

        for (iterationNr = 0; iterationNr < maxIterations; iterationNr++) {
            // z^2 + c
            newx = (x * x) - (y * y) + juliaX;
            newy = (2 * x * y) + juliaY;

            x = newx;
            y = newy;

            // Well known result: if distance is >2, escapes to infinity...
            if ((x * x + y * y) > 4) {
                inside = false;
                break;
            }
        }

        if (inside)
            return this.colourStrategy.colourInsidePoint();
        else
            return this.colourStrategy.colourOutsidePoint(iterationNr, maxIterations);
    }

    @Override
    public boolean shouldPerformCrudeFirst() {
        return false;
    }
}
