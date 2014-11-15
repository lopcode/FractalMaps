package uk.ac.ed.inf.mandelbrotmaps.refactor.strategies;

import android.util.Log;

import uk.ac.ed.inf.mandelbrotmaps.MandelbrotJuliaLocation;
import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeStrategy;

public abstract class FractalComputeStrategy implements IFractalComputeStrategy {
    protected int width;
    protected int height;
    protected IFractalComputeDelegate delegate;
    protected IColourStrategy colourStrategy;
    protected int[] pixelSizes;
    protected int[] fractalPixels;

    protected double[] graphArea; // {x_min, y_max, width}
    protected double[] homeGraphArea;

    protected double detail;

    // Default pixel block sizes for crude, detailed renders
    protected final int CRUDE_PIXEL_BLOCK = 3;
    protected final int DEFAULT_PIXEL_SIZE = 1;

    // How many iterations, at the very fewest, will we do?
    protected int MIN_ITERATIONS = 10;

    // Constants for iteration number calculations
    protected static final double LITTLE_DETAIL_BOOST = 1.5; //Need to bump up the scaling on the little view so it looks better.
    protected static final double DETAIL_DIVISOR = 50;
    protected static final double DEFAULT_DETAIL_LEVEL = 15;

    protected double ITERATION_BASE;
    protected double ITERATION_CONSTANT_FACTOR;

    // Level of detail (abstracted for convenience - dividing by 100 gets the useful number).
    protected double detailLevel = 30;

    // How often to redraw fractal when rendering. Set to 1/12th screen size in onSizeChanged()
    protected int linesToDrawAfter = 20; // This default value normally isn't used.

    // Render calculating variables
    double xMin, yMax, pixelSize;

    @Override
    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        this.width = width;
        this.height = height;
        this.delegate = delegate;

        this.initialisePixelBuffers();

        // Set the "maximum iteration" calculation constants
        // Empirically determined values for Mandelbrot set.
        ITERATION_BASE = 1.24;
        ITERATION_CONSTANT_FACTOR = 54;

        // Set home area
        homeGraphArea = new MandelbrotJuliaLocation().getMandelbrotGraphArea();
    }

    public void setFractalDetail(double detail) {
        this.detail = detail;
    }

    public void setColourStrategy(IColourStrategy colourStrategy) {
        this.colourStrategy = colourStrategy;
    }

    int getMaxIterations() {
        // How many iterations to perform?
        double absLnPixelSize = Math.abs(Math.log(getPixelSize()));

        double dblIterations = (this.detail / DETAIL_DIVISOR) * ITERATION_CONSTANT_FACTOR * Math.pow(ITERATION_BASE, absLnPixelSize);

        int iterationsToPerform = (int) dblIterations;

        return Math.max(iterationsToPerform, MIN_ITERATIONS);
    }

    /* Compute length of 1 pixel on the complex plane */
    double getPixelSize() {
        // Nothing to do - cannot compute a sane pixel size
        if (this.width == 0) return 0.0;
        if (graphArea == null) return 0.0;

        // Return the pixel size
        return (graphArea[2] / (double) this.width);
    }

    @Override
    public int[] getFractalResult() {
        return this.fractalPixels;
    }

    @Override
    public int[] getPixelSizes() {
        return this.pixelSizes;
    }

    public void initialisePixelBuffers() {
        this.fractalPixels = new int[this.width * this.height];
        this.pixelSizes = new int[this.width * this.height];
        this.clearPixelSizes();
    }

    public void clearPixelSizes() {
        for (int i = 0; i < this.pixelSizes.length; i++) {
            this.pixelSizes[i] = 1000;
        }
    }

    public void translateFractal(int x, int y) {
        int[] newPixels = new int[height * width];
        int[] newSizes = new int[height * width];
        for (int i = 0; i < newSizes.length; i++) newSizes[i] = 1000;

        //Choose rows to copy from
        int rowNum = height - Math.abs(y);
        int origStartRow = (y < 0 ? Math.abs(y) : 0);

        //Choose columns to copy from
        int colNum = width - Math.abs(x);
        int origStartCol = (x < 0 ? Math.abs(x) : 0);

        //Choose columns to copy to
        int destStartCol = (x < 0 ? 0 : x);

        //Copy useful parts into new array
        for (int origY = origStartRow; origY < origStartRow + rowNum; origY++) {
            int destY = origY + y;
            System.arraycopy(fractalPixels, (origY * width) + origStartCol,
                    newPixels, (destY * width) + destStartCol,
                    colNum);
            System.arraycopy(pixelSizes, (origY * width) + origStartCol,
                    newSizes, (destY * width) + destStartCol,
                    colNum);
        }

        //Set values
        fractalPixels = newPixels;
        pixelSizes = newSizes;

    }

    public void setGraphArea(double[] newGraphArea) {
        graphArea = newGraphArea;
        Log.i("FCS", "Setting new graph area to " + newGraphArea[0] + " " + newGraphArea[1] + " " + newGraphArea[2]);
    }

    // Abstract methods

    abstract int pixelInSet(int xPixel, int yPixel, int maxIterations);
}
