package uk.ac.ed.inf.mandelbrotmaps.presenter;

import android.content.Context;
import android.graphics.Matrix;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ed.inf.mandelbrotmaps.IFractalSceneDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;
import uk.ac.ed.inf.mandelbrotmaps.compute.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.IFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.overlay.IFractalOverlay;
import uk.ac.ed.inf.mandelbrotmaps.touch.IFractalTouchDelegate;
import uk.ac.ed.inf.mandelbrotmaps.touch.IFractalTouchHandler;
import uk.ac.ed.inf.mandelbrotmaps.view.IFractalView;
import uk.ac.ed.inf.mandelbrotmaps.view.IViewResizeListener;

public class FractalPresenter implements IFractalPresenter, IFractalComputeDelegate, IFractalTouchDelegate, IViewResizeListener {
    public IFractalComputeStrategy fractalStrategy;
    public IFractalView view;
    public IFractalTouchHandler touchHandler;
    private IFractalSceneDelegate sceneDelegate;

    private Context context;

    Matrix transformMatrix;

    private int[] pixelBuffer;
    private int[] pixelBufferSizes;
    private double[] graphArea;

    private int viewWidth;
    private int viewHeight;

    protected double detail;

    private long lastComputeStart;

    private final int ZOOM_SLIDER_SCALING = 300;
    private double MINZOOM_LN_PIXEL = -3;

    // Touch
    boolean hasZoomed;

    // Default pixel block sizes for crude, detailed renders
    public static final int CRUDE_PIXEL_BLOCK = 3;
    public static final int DEFAULT_PIXEL_SIZE = 1;

    // How many iterations, at the very fewest, will we do?
    protected int MIN_ITERATIONS = 10;

    // Constants for iteration number calculations
    protected static final double LITTLE_DETAIL_BOOST = 1.5; //Need to bump up the scaling on the little view so it looks better.
    protected static final double DETAIL_DIVISOR = 50;

    // Overlays
    private List<IFractalOverlay> fractalPresenterOverlays;

    public FractalPresenter(Context context, IFractalSceneDelegate sceneDelegate, IFractalComputeStrategy fractalStrategy) {
        this.context = context;
        this.fractalStrategy = fractalStrategy;

        this.transformMatrix = new Matrix();
        this.sceneDelegate = sceneDelegate;

        this.initialiseOverlays();
    }

    public void initialiseOverlays() {
        this.fractalPresenterOverlays = new ArrayList<IFractalOverlay>();
    }

    // IFractalPresenter

    @Override
    public void setTouchHandler(IFractalTouchHandler touchHandler) {
        this.touchHandler = touchHandler;
    }

    @Override
    public void setComputeStrategy(IFractalComputeStrategy strategy) {
        this.fractalStrategy = strategy;
    }

    @Override
    public void setFractalDetail(double detail) {
        this.detail = detail;
    }

    @Override
    public void setView(IFractalView view, Matrix matrix, IViewResizeListener listener) {
        this.view = view;
        this.view.setFractalTransformMatrix(matrix);
        this.view.setResizeListener(this);
        this.view.setTouchHandler(this.touchHandler);
        this.view.setPresenterOverlays(this.fractalPresenterOverlays);

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
        Log.i("AFV", "Starting new style render");
        Log.i("AFV", "Notifying of update every " + this.viewHeight / 6 + " lines");

        if (pixelBlockSize == DEFAULT_PIXEL_SIZE)
            this.sceneDelegate.setRenderingStatus(this, true);

        this.lastComputeStart = System.nanoTime();

        this.fractalStrategy.computeFractal(new FractalComputeArguments(pixelBlockSize,
                this.getMaxIterations(),
                this.viewHeight / 6,
                DEFAULT_PIXEL_SIZE,
                this.viewWidth,
                this.viewHeight,
                graphArea[0],
                graphArea[1],
                getPixelSize(),
                this.pixelBuffer,
                this.pixelBufferSizes));

        this.sceneDelegate.onFractalRecomputeScheduled(this);
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

    public int getZoomLevel() {
        double pixelSize = getPixelSize();

        // If the pixel size = 0, something's wrong (happens at Julia launch).
        if (pixelSize == 0.0d)
            return 1;

        double lnPixelSize = Math.log(pixelSize);
        double zoomLevel = (double) ZOOM_SLIDER_SCALING * (lnPixelSize - MINZOOM_LN_PIXEL) / (this.fractalStrategy.getMaxZoomLevel() - MINZOOM_LN_PIXEL);
        return (int) zoomLevel;
    }

    /* Checks if this zoom level if sane (within the chosen limits) */
    boolean saneZoomLevel() {
        int zoomLevel = getZoomLevel();

        if ((zoomLevel >= 1) && (zoomLevel <= ZOOM_SLIDER_SCALING)) {
            return true;
        } else {
            return false;
        }
    }

    public void initialisePixelBuffers() {
        this.pixelBuffer = new int[this.viewWidth * this.viewHeight];
        this.pixelBufferSizes = new int[this.viewWidth * this.viewHeight];
        this.clearPixelSizes();
    }

    // Reset pixel sizes to force a full render on the next pass
    public void clearPixelSizes() {
        Log.i("FP", "Clearing pixel sizes");
        for (int i = 0; i < this.pixelBufferSizes.length; i++) {
            this.pixelBufferSizes[i] = 1000;
        }
    }

    @Override
    public void setGraphArea(double[] graphArea) {
        this.graphArea = graphArea;
    }

    @Override
    public double[] getGraphArea() {
        return this.graphArea;
    }

    @Override
    public void notifyRecomputeComplete(int pixelBlockSize) {
        long timeDifference = System.nanoTime() - this.lastComputeStart;
        double timeInSeconds = timeDifference / 1000000000.0D;

        if (pixelBlockSize == DEFAULT_PIXEL_SIZE) {
            this.sceneDelegate.setRenderingStatus(this, false);

            this.sceneDelegate.onFractalRecomputed(this, timeInSeconds);
        }
    }

    public double[] getGraphPositionFromClickedPosition(float touchX, float touchY) {
        double pixelSize = getPixelSize();

        double[] graphPosition = new double[2];

        // Touch position, on the complex plane (translated from pixels)
        graphPosition[0] = graphArea[0] + ((double) touchX * pixelSize);
        graphPosition[1] = graphArea[1] - ((double) touchY * pixelSize);

        return graphPosition;
    }

    @Override
    public double[] getPointFromGraphPosition(double pointX, double pointY) {
        double[] point = new double[2];
        double pixelSize = getPixelSize();
        point[0] = ((pointX - graphArea[0]) / pixelSize);
        point[1] = (-(pointY - graphArea[1]) / pixelSize);

        return point;
    }

    @Override
    public void initialiseStrategy() {
        this.fractalStrategy.initialise(this.viewWidth, this.viewHeight, this);
    }

    // IFractalComputeDelegate

    @Override
    public void postUpdate(int[] pixels, int[] pixelSizes) {
        Log.i("FP", "Got compute update");
        this.pixelBuffer = pixels;
        this.pixelBufferSizes = pixelSizes;
        this.view.setBitmapPixels(this.pixelBuffer);
        this.view.postThreadSafeRedraw();
    }

    @Override
    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize) {
        Log.i("AFV", "Render finished");
        this.postUpdate(pixels, pixelSizes);

        this.notifyRecomputeComplete(pixelBlockSize);
    }

    @Override
    public void onComputeStarted(int pixelBlockSize) {
        if (pixelBlockSize == DEFAULT_PIXEL_SIZE)
            this.sceneDelegate.setRenderingStatus(this, true);
    }

    // IFractalTouchDelegate

    @Override
    public void startDragging() {
        Log.i("FP", "Started dragging");

        this.fractalStrategy.stopAllRendering();

        this.transformMatrix.reset();
        this.view.setFractalTransformMatrix(this.transformMatrix);

        this.hasZoomed = false;
    }

    @Override
    public void dragFractal(float x, float y, float totalDragX, float totalDragY) {
        this.transformMatrix.postTranslate(x, y);
        this.view.setFractalTransformMatrix(this.transformMatrix);

        this.view.postUIThreadRedraw();
    }

    public void stopDragging(boolean stoppedOnZoom, float totalDragX, float totalDragY) {
        Log.i("FP", "Stopped dragging: " + totalDragX + " " + totalDragY);

        if (totalDragX < -this.viewWidth)
            totalDragX = -this.viewWidth;

        if (totalDragX > this.viewWidth)
            totalDragX = this.viewWidth;

        if (totalDragY < -this.viewHeight)
            totalDragY = -this.viewHeight;

        if (totalDragY > this.viewHeight)
            totalDragY = this.viewHeight;

        if (!hasZoomed && !stoppedOnZoom) {
            this.translatePixelBuffer((int) totalDragX, (int) totalDragY);
            this.view.setBitmapPixels(this.pixelBuffer);
        }

        this.translateGraphArea((int) totalDragX, (int) totalDragY);

        if (!stoppedOnZoom) {
            this.clearPixelSizes();
            this.setGraphArea(graphArea);
            this.sceneDelegate.scheduleRecomputeBasedOnPreferences(this);
        }

        if (!hasZoomed && !stoppedOnZoom) {
            this.transformMatrix.reset();
            this.view.setFractalTransformMatrix(this.transformMatrix);
        }

        this.hasZoomed = false;
        this.view.postUIThreadRedraw();
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

        this.transformMatrix.reset();
    }

    @Override
    public void scaleFractal(float scaleFactor, float midX, float midY) {
        if (!this.saneZoomLevel()) {
            Log.w("FP", "Zoom level not sane!");
        }

        this.zoomGraphArea((int) midX, (int) midY, 1 / scaleFactor);

        this.transformMatrix.postScale(scaleFactor, scaleFactor, midX, midY);
        this.view.setFractalTransformMatrix(this.transformMatrix);

        this.view.postUIThreadRedraw();
    }

    @Override
    public void onLongClick(float x, float y) {
        this.sceneDelegate.onFractalLongClick(this, x, y);
    }

    // IViewResizeListener

    @Override
    public void onViewResized(View view, int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;

        this.initialisePixelBuffers();
        this.initialiseStrategy();
        this.view.createNewFractalBitmap(new int[this.viewWidth * this.viewHeight]);

        this.sceneDelegate.onFractalViewReady(this);
    }

    // IFractalPresenterDelegate

    @Override
    public void onSceneOverlaysChanged(List<IFractalOverlay> overlays) {
        Log.i("FP", "Changing scene overlays");
        this.view.setSceneOverlays(overlays);
    }
}
