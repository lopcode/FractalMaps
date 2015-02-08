package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.preference.PreferenceManager;

public class JuliaFractalView extends AbstractFractalView {
    // Point paramaterising this Julia set
    private double juliaX = 0;
    private double juliaY = 0;
    private float[] pointOneCoords = new float[2];
    private float[] pointTwoCoords = new float[2];
    private float[] pointThreeCoords = new float[2];

    Paint pointOnePaint;
    Paint pointTwoPaint;
    Paint pointThreePaint;

    int pointOneAlpha = 150;
    int pointTwoAlpha = 150;
    int pointThreeAlpha = 150;

    public JuliaFractalView(Context context, FractalViewSize size) {
        super(context, size);

        setColouringScheme(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("JULIA_COLOURS", "JuliaDefault"), false);

        for (int i = 0; i < noOfThreads; i++) {
            renderThreadList.get(i).setName("Julia thread " + i);
        }

        // Set the "maximum iteration" calculation constants
        // Empirically determined values for Julia sets.
        ITERATION_BASE = 1.58;
        ITERATION_CONSTANT_FACTOR = 6.46;

        // Set home area
        homeGraphArea = new MandelbrotJuliaLocation().getJuliaGraphArea();

        // How deep a zoom do we allow?
        MAXZOOM_LN_PIXEL = -20; // Beyond -21, "double"s break down(!).

        int pinColour = Color.parseColor(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PIN_COLOUR", "blue"));
        int fixedPointColour = Color.parseColor("green");
        if (fixedPointColour == pinColour) { fixedPointColour = Color.parseColor("blue"); }

        pointOnePaint = new Paint();
        pointOnePaint.setColor(pinColour);
        pointOnePaint.setAlpha(pointOneAlpha);
        pointOnePaint.setStyle(Paint.Style.STROKE);

        pointTwoPaint = new Paint();
        pointTwoPaint.setColor(fixedPointColour);
        pointTwoPaint.setAlpha(pointTwoAlpha);
        pointTwoPaint.setStyle(Paint.Style.STROKE);

        pointThreePaint = new Paint();
        pointThreePaint.setColor(fixedPointColour);
        pointThreePaint.setAlpha(pointThreeAlpha);
        pointThreePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (parentActivity.tanLeiEnabled && !parentActivity.TLPointSelected) {
            if (controlmode != ControlMode.ZOOMING) {
                if (juliaX == (double) parentActivity.misPoints[0][0] &&
                        juliaY == (double) parentActivity.misPoints[0][1]) {
                    // first Tan Lei point always at the seed
                    pointOneCoords = convertCoordsToPixels(new float[]{(float) juliaX, (float) juliaY});
                    pointTwoCoords = convertCoordsToPixels(parentActivity.centerPoints[0]);
                    pointThreeCoords = convertCoordsToPixels(parentActivity.centerPoints[1]);
                } else if (juliaX == (double) parentActivity.misPoints[1][0] &&
                        juliaY == (double) parentActivity.misPoints[1][1]) {
                    // first Tan Lei point always at the seed
                    pointOneCoords = convertCoordsToPixels(new float[]{(float) juliaX, (float) juliaY});
                    pointTwoCoords = convertCoordsToPixels(parentActivity.centerPoints[2]);
                    pointThreeCoords = convertCoordsToPixels(parentActivity.centerPoints[3]);
                } else if (juliaX == (double) parentActivity.misPoints[2][0] &&
                        juliaY == (double) parentActivity.misPoints[2][1]) {
                    // first Tan Lei point always at the seed
                    pointOneCoords = convertCoordsToPixels(new float[]{(float) juliaX, (float) juliaY});
                    pointTwoCoords = convertCoordsToPixels(parentActivity.centerPoints[4]);
                    pointThreeCoords = convertCoordsToPixels(parentActivity.centerPoints[5]);
                }
            }
            float[] mappedCoordsTL1 = new float[2];
            float[] mappedCoordsTL2 = new float[2];
            float[] mappedCoordsTL3 = new float[2];

            if (fractalViewSize == FractalViewSize.LARGE) {

                matrix.mapPoints(mappedCoordsTL1, pointOneCoords);

                pointOnePaint.setStrokeWidth(10);
                canvas.drawRect(mappedCoordsTL1[0] - pointBoxWidth / 2, mappedCoordsTL1[1] - pointBoxHeight / 2,
                        mappedCoordsTL1[0] + pointBoxWidth / 2, mappedCoordsTL1[1] + pointBoxHeight / 2, pointOnePaint);

                matrix.mapPoints(mappedCoordsTL2, pointTwoCoords);

                pointTwoPaint.setStrokeWidth(10);
                canvas.drawRect(mappedCoordsTL2[0] - pointBoxWidth / 2, mappedCoordsTL2[1] - pointBoxHeight / 2,
                        mappedCoordsTL2[0] + pointBoxWidth / 2, mappedCoordsTL2[1] + pointBoxHeight / 2, pointTwoPaint);

                matrix.mapPoints(mappedCoordsTL3, pointThreeCoords);

                pointThreePaint.setStrokeWidth(10);
                canvas.drawRect(mappedCoordsTL3[0] - pointBoxWidth / 2, mappedCoordsTL3[1] - pointBoxHeight / 2,
                        mappedCoordsTL3[0] + pointBoxWidth / 2, mappedCoordsTL3[1] + pointBoxHeight / 2, pointThreePaint);
            }
        }
    }

    /* Runs when the view changes size.
     * Sets the size of the pin and box based on screen size. */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (fractalViewSize == FractalViewSize.LARGE) {
            pointBoxHeight = getHeight() / 6;
            pointBoxWidth = getWidth() / 6;
            switchBoxHeight = getHeight() / 9;
            switchBoxWidth = getWidth() / 9;
        }
    }

    public void setJuliaParameter(double newJuliaX, double newJuliaY) {
        //stopAllRendering();
        juliaX = newJuliaX;
        juliaY = newJuliaY;
        setGraphArea(graphArea, true);
    }

    public double[] getJuliaParam() {
        double[] juliaParam = new double[2];
        juliaParam[0] = juliaX;
        juliaParam[1] = juliaY;
        return juliaParam;
    }

    // Load a location
    void loadLocation(MandelbrotJuliaLocation mjLocation) {
        //setScaledIterationCount(mjLocation.getJuliaContrast());
        double[] juliaParam = mjLocation.getJuliaParam();
        setGraphArea(mjLocation.getJuliaGraphArea(), true);
        setJuliaParameter(juliaParam[0], juliaParam[1]);
    }

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
            return colourer.colourInsidePoint();
        else
            return colourer.colourOutsidePoint(iterationNr, maxIterations);
    }
}
