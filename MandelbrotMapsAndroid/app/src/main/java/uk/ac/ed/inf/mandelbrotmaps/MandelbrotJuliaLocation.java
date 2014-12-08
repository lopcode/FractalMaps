package uk.ac.ed.inf.mandelbrotmaps;

public class MandelbrotJuliaLocation {
    private double[] mandelbrotGraphArea;
    private double[] juliaGraphArea;
    private double[] juliaParams;

    public static final double[] defaultMandelbrotGraphArea = new double[]{-3.1, 1.45, 5};
    public static final double[] defaultJuliaGraphArea = new double[]{-2.2, 1.25, 4.3};
    public static final double[] defaultJuliaParams = new double[]{-0.6, -0.01875}; //Julia params place it right in the middle of the Mandelbrot home.

    // Constructor. Defaults - some semi-arbitrary, pretty values
    public MandelbrotJuliaLocation() {
        mandelbrotGraphArea = defaultMandelbrotGraphArea;
        juliaGraphArea = defaultJuliaGraphArea;
        juliaParams = defaultJuliaParams;
    }

    public MandelbrotJuliaLocation(double[] _mandelbrotGraphArea, double[] _juliaGraphArea, double[] _juliaParams) {
        mandelbrotGraphArea = _mandelbrotGraphArea;
        juliaGraphArea = _juliaGraphArea;
        juliaParams = _juliaParams;
    }

    public double[] getMandelbrotGraphArea() {
        return mandelbrotGraphArea;
    }

    public double[] getJuliaGraphArea() {
        return juliaGraphArea;
    }

    public double[] getJuliaParam() {
        return juliaParams;
    }


    public String toString() {
        String outString = "";

        for (double d : mandelbrotGraphArea) {
            outString += (Double.toString(d) + " ");
        }

        for (double d : juliaGraphArea) {
            outString += (Double.toString(d) + " ");
        }

        for (double d : juliaParams) {
            outString += (Double.toString(d) + " ");
        }

        return outString;
    }
}