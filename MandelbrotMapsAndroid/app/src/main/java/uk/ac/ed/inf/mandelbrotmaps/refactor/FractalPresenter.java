package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.graphics.Matrix;
import android.util.Log;
import android.view.View;

import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.IFractalComputeStrategy;

public class FractalPresenter implements IFractalPresenter, IFractalComputeDelegate, IFractalTouchDelegate, IViewResizeListener {
    public IFractalComputeStrategy fractalStrategy;
    public IFractalView view;

    Matrix transformMatrix;

    private int[] pixelBuffer;
    private int[] pixelBufferSizes;
    private double[] graphArea;

    private int viewWidth;
    private int viewHeight;

    protected double detail;

    // Touch

    boolean hasZoomed;

    public float totalDragX = 0;
    public float totalDragY = 0;

    // Default pixel block sizes for crude, detailed renders
    public static final int CRUDE_PIXEL_BLOCK = 3;
    public static final int DEFAULT_PIXEL_SIZE = 1;

    // How many iterations, at the very fewest, will we do?
    protected int MIN_ITERATIONS = 10;

    // Constants for iteration number calculations
    protected static final double LITTLE_DETAIL_BOOST = 1.5; //Need to bump up the scaling on the little view so it looks better.
    protected static final double DETAIL_DIVISOR = 50;
    public static final double DEFAULT_DETAIL_LEVEL = 15;

    public FractalPresenter(IFractalComputeStrategy fractalStrategy) {
        this.fractalStrategy = fractalStrategy;

        this.transformMatrix = new Matrix();
    }

    // IFractalPresenter

    @Override
    public void setFractalDetail(double detail) {
        this.detail = detail;
    }

    @Override
    public int[] getPixelBuffer() {
        return this.pixelBuffer;
    }

    @Override
    public void translatePixelBuffer(int x, int y) {
        int[] newPixels = new int[this.viewWidth * this.viewHeight];
        int[] newSizes = new int[this.viewWidth * this.viewHeight];
        for (int i = 0; i < newSizes.length; i++) newSizes[i] = 1000;

        //Choose rows to copy from
        int rowNum = this.viewHeight - Math.abs(y);
        int origStartRow = (y < 0 ? Math.abs(y) : 0);

        //Choose columns to copy from
        int colNum = this.viewWidth - Math.abs(x);
        int origStartCol = (x < 0 ? Math.abs(x) : 0);

        //Choose columns to copy to
        int destStartCol = (x < 0 ? 0 : x);

        //Copy useful parts into new array
        for (int origY = origStartRow; origY < origStartRow + rowNum; origY++) {
            int destY = origY + y;
            System.arraycopy(this.pixelBuffer, (origY * this.viewWidth) + origStartCol,
                    newPixels, (destY * this.viewWidth) + destStartCol,
                    colNum);
            System.arraycopy(this.pixelBufferSizes, (origY * this.viewWidth) + origStartCol,
                    newSizes, (destY * this.viewWidth) + destStartCol,
                    colNum);
        }

        this.pixelBuffer = newPixels;
        this.pixelBufferSizes = newSizes;
    }

    @Override
    public void recomputeGraph(int pixelBlockSize) {
        int threadID = 0;
        int noOfThreads = 1;

        int yStart = (this.viewHeight / 2) + (threadID * pixelBlockSize);
        int yEnd = this.viewHeight - (noOfThreads - (threadID + 1));
        boolean showRenderProgress = (threadID == 0);

        Log.i("AFV", "Starting new style render");

        this.fractalStrategy.computeFractal(pixelBlockSize,
                false,
                this.getMaxIterations(),
                12,
                DEFAULT_PIXEL_SIZE,
                0,
                this.viewWidth,
                yStart,
                yEnd,
                graphArea[0],
                graphArea[1],
                getPixelSize(),
                this.pixelBuffer,
                this.pixelBufferSizes);
    }

    @Override
    public void translateGraphArea(int dx, int dy) {
        //Log.d(TAG, "moveFractal()");

        // What does each pixel correspond to, on the complex plane?
        double pixelSize = getPixelSize();

        // Adjust the Graph Area
        double[] newGraphArea = graphArea;
        newGraphArea[0] -= (dx * pixelSize);
        newGraphArea[1] -= -(dy * pixelSize);
        this.setGraphArea(newGraphArea);
    }

    @Override
    public void zoomGraphArea(int x, int y, double scale) {

        double pixelSize = getPixelSize();

        double[] oldGraphArea = graphArea;
        double[] newGraphArea = new double[3];

        double zoomPercentChange = (double) scale; //= (double)(100 + (zoomAmount)) / 100;

        // What is the zoom centre?
        double zoomCentreX = oldGraphArea[0] + ((double) x * pixelSize);
        double zoomCentreY = oldGraphArea[1] - ((double) y * pixelSize);

        // Since we're zooming in on a point (the "zoom centre"),
        // let's now shrink each of the distances from the zoom centre
        // to the edges of the picture by a constant percentage.
        double newMinX = zoomCentreX - (zoomPercentChange * (zoomCentreX - oldGraphArea[0]));
        double newMaxY = zoomCentreY - (zoomPercentChange * (zoomCentreY - oldGraphArea[1]));

        double oldMaxX = oldGraphArea[0] + oldGraphArea[2];
        double newMaxX = zoomCentreX - (zoomPercentChange * (zoomCentreX - oldMaxX));

        double leftWidthDiff = newMinX - oldGraphArea[0];
        double rightWidthDiff = oldMaxX - newMaxX;

        newGraphArea[0] = newMinX;
        newGraphArea[1] = newMaxY;
        newGraphArea[2] = oldGraphArea[2] - leftWidthDiff - rightWidthDiff;

        //Log.d(TAG, "Just zoomed - zoom level is " + getZoomLevel());

        setGraphArea(newGraphArea);
    }

    public int getMaxIterations() {
        double absLnPixelSize = Math.abs(Math.log(getPixelSize()));

        Log.i("FP", "Max iterations: " + absLnPixelSize);
        double dblIterations = (this.detail / DETAIL_DIVISOR) * this.fractalStrategy.getIterationConstantFactor() * Math.pow(this.fractalStrategy.getIterationBase(), absLnPixelSize);

        int iterationsToPerform = (int) dblIterations;

        return Math.max(iterationsToPerform, MIN_ITERATIONS);
    }

    // Compute length of 1 pixel on the complex plane
    public double getPixelSize() {
        // Nothing to do - cannot compute a sane pixel size
        if (this.viewWidth == 0) return 0.0;
        if (graphArea == null) return 0.0;

        // Return the pixel size
        return (graphArea[2] / (double) this.viewWidth);
    }

    public void initialisePixelBuffers() {
        this.pixelBuffer = new int[this.viewWidth * this.viewHeight];
        this.pixelBufferSizes = new int[this.viewWidth * this.viewHeight];
        this.clearPixelSizes();
    }

    // Reset pixel sizes to force a full render on the next pass
    public void clearPixelSizes() {
        for (int i = 0; i < this.pixelBufferSizes.length; i++) {
            this.pixelBufferSizes[i] = 1000;
        }
    }

    public void setGraphArea(double[] graphArea) {
        this.graphArea = graphArea;
    }

    // IFractalComputeDelegate

    @Override
    public void postUpdate(int[] pixels, int[] pixelSizes) {
        this.pixelBuffer = pixels;
        this.pixelBufferSizes = pixelSizes;
        this.view.createNewFractalBitmap(this.pixelBuffer);
        this.view.redraw();
    }

    @Override
    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize) {
        Log.i("AFV", "Render finished");
        this.postUpdate(pixels, pixelSizes);

//        this.notifyCompleteRender(0, pixelBlockSize);
    }

    // IFractalTouchDelegate

    @Override
    public void startDragging() {
        Log.i("FP", "Started dragging");

        //Stop current rendering (to not render areas that are offscreen afterwards)
//        stopAllRendering();

        this.totalDragX = 0;
        this.totalDragY = 0;

        this.transformMatrix.reset();
        this.view.setFractalTransformMatrix(this.transformMatrix);

        this.hasZoomed = false;
    }

    public void stopDragging(boolean stoppedOnZoom) {
        Log.i("FP", "Stopped dragging: " + totalDragX + " " + totalDragY);
        this.translatePixelBuffer((int) totalDragX, (int) totalDragY);

        if (!hasZoomed && !stoppedOnZoom) {
            //Set the new location for the fractals
            this.translateGraphArea((int) totalDragX, (int) totalDragY);
        }

        this.setGraphArea(graphArea);
        if (!stoppedOnZoom)
            this.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);

        this.transformMatrix.reset();
        this.view.setFractalTransformMatrix(this.transformMatrix);

        // Reset all the variables (possibly paranoid)
        if (!hasZoomed && !stoppedOnZoom) {
            this.transformMatrix.reset();
            this.view.setFractalTransformMatrix(this.transformMatrix);
        }

        this.hasZoomed = false;

        this.view.redraw();
    }

    public void startScaling(float x, float y) {
        Log.i("FP", "Started scaling");
        hasZoomed = true;
        this.clearPixelSizes();
    }

    public void stopScaling() {
        Log.i("FP", "Stopped scaling");
        this.clearPixelSizes();

        this.view.cacheCurrentBitmap(this.pixelBuffer);

        totalDragX = 0;
        totalDragY = 0;
        this.transformMatrix.reset();
    }

    @Override
    public void scaleFractal(float scaleFactor, float midX, float midY) {
        this.zoomGraphArea((int) midX, (int) midY, 1 / scaleFactor);

        this.transformMatrix.postScale(scaleFactor, scaleFactor, midX, midY);
        this.view.setFractalTransformMatrix(this.transformMatrix);
        this.view.redraw();
    }

    @Override
    public void dragFractal(float x, float y) {
        totalDragX += x;
        totalDragY += y;

        this.transformMatrix.postTranslate(x, y);
        this.view.setFractalTransformMatrix(this.transformMatrix);
        this.view.redraw();
    }

    // IViewResizeListener

    @Override
    public void onViewResized(View view, int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;

        this.initialisePixelBuffers();
        this.fractalStrategy.initialise(this.viewWidth, this.viewHeight, this);
        this.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }
}