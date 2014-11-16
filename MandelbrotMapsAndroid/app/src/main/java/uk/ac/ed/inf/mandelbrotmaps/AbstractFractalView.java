package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.ed.inf.mandelbrotmaps.colouring.DefaultColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.JuliaColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.PsychadelicColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.RGBWalkColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.refactor.FractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.FractalComputeStrategy;

public abstract class AbstractFractalView extends View implements IFractalComputeDelegate {
    // How many different discrete zoom levels?
    public final int ZOOM_SLIDER_SCALING = 300;

    // Tracks current control (zooming, dragging, or none)
    public static enum ControlMode {
        ZOOMING,
        DRAGGING,
        STATIC
    }

    public ControlMode controlmode = ControlMode.STATIC;

    // Lists for threads, their queues, and their status
    int noOfThreads = 1;
    ArrayList<LinkedBlockingQueue<Rendering>> renderQueueList = new ArrayList<LinkedBlockingQueue<Rendering>>();
    ArrayList<RenderThread> renderThreadList = new ArrayList<RenderThread>();
    ArrayList<Boolean> rendersComplete = new ArrayList<Boolean>();

    // What zoom range do we allow? Expressed as ln(pixelSize).
    double MINZOOM_LN_PIXEL = -3;
    double MAXZOOM_LN_PIXEL;

    // Graph area on the complex plane? Stored as double[] {x_min, y_max, width}
    double[] graphArea;
    double[] homeGraphArea;

    // Fractal image data
    int[] fractalPixels;
    int[] pixelSizes;
    Bitmap fractalBitmap;

    // Image position on screen (changes while dragging)
    public float bitmapX = 0;
    public float bitmapY = 0;

    // Dragging state
    public float totalDragX = 0;
    public float totalDragY = 0;
    public boolean holdingPin = false;

    // Scaling state
    private float totalScaleFactor = 1.0f;
    public float scaleFactor = 1.0f;
    public float midX = 0.0f;
    public float midY = 0.0f;

    boolean hasZoomed = false;
    boolean hasPassedMaxDepth = false;

    // Tracks scaling/ dragging position
    public Matrix matrix;

    // Handle to activity holding the view
    public FractalActivity parentActivity;

    // Used to track length of a render
    private long renderStartTime;

    // Track number of times bitmap is recreated onDraw (debug info)
    int bitmapCreations = 0;

    boolean drawPin = true;

    public IColourStrategy colourer = new DefaultColourStrategy();

    boolean completedLastRender = false;

    // Render calculating variables
    double xMin, yMax, pixelSize;

    FractalComputeStrategy strategy;

    public AbstractFractalView(Context context) {
        super(context);

        //this(context, FractalViewSize.LARGE);
    }

    /*-----------------------------------------------------------------------------------*/
    /*Constructor*/
    /*-----------------------------------------------------------------------------------*/
    /* Constructor for the view, assigns the parent activity and size and
     * launches the render threads. */
    public AbstractFractalView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setFocusable(true);
        setFocusableInTouchMode(true);
        //setId(0);
        setBackgroundColor(Color.BLACK);
    }

    public void initialise(FractalActivity parentActivity) {
        this.parentActivity = parentActivity;
        setOnTouchListener(parentActivity);
        setOnLongClickListener(parentActivity);
        setLongClickable(true);

        //Up the iteration count a bit for the little view (decent value, seems to work)
          /*if (fractalViewSize == FractalViewSize.LITTLE) {
              iterationScaling *= 1.5;
      	}*/

        matrix = new Matrix();
        matrix.reset();

        this.initialiseRenderThreads();
    }

    public void initialiseRenderThreads() {
        // Create the render threads
        //noOfThreads = Runtime.getRuntime().availableProcessors();
        noOfThreads = 1;
        //Log.d(TAG, "Using " + noOfThreads + " cores");

        for (int i = 0; i < noOfThreads; i++) {
            rendersComplete.add(false);
            renderQueueList.add(new LinkedBlockingQueue<Rendering>());
            renderThreadList.add(new RenderThread(this, i, noOfThreads));
            renderThreadList.get(i).start();
        }
    }

    /*-----------------------------------------------------------------------------------*/
    /*Android life-cycle handling*/
    /*-----------------------------------------------------------------------------------*/
    /* Runs when the view changes size.
     * Used to set the little fractal view once large fractal view size has first been determined.
	 * Also sets linesToDrawAfter to 1/12th of the screen size. */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        strategy.initialise(w, h, this);
//        strategy.setFractalDetail(parentActivity.getDetailFromPrefs(fractalViewSize));
    }

    /*-----------------------------------------------------------------------------------*/
    /* Fractal drawing */
    /*-----------------------------------------------------------------------------------*/
    /* Draw the fractal Bitmap to the screen, updating it if not dragging or zooming */
    @Override
    protected void onDraw(Canvas canvas) {
        // (Re)create pixel grid, if not initialised - or if wrong size.
        if ((fractalPixels == null) || (fractalPixels.length != getWidth() * getHeight())) {
            fractalPixels = new int[getWidth() * getHeight()];
            pixelSizes = new int[getWidth() * getHeight()];
            clearPixelSizes();
            //scheduleNewRenders();
            setGraphArea(graphArea, true);
        }

        //Translation
        if (controlmode == ControlMode.DRAGGING) {
            matrix.postTranslate(bitmapX, bitmapY);
            bitmapX = 0;
            bitmapY = 0;
        }

        //Scaling
        matrix.postScale(scaleFactor, scaleFactor, midX, midY);
        scaleFactor = 1.0f;
        midX = 0.0f;
        midY = 0.0f;

        //Create new image only if not dragging, zooming, or moving the Julia pin
        if (controlmode == ControlMode.STATIC && !holdingPin && getWidth() > 0) {
            bitmapCreations++;
            fractalBitmap = Bitmap.createBitmap(this.fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
        }

        //Draw fractal image on screen
        if (getWidth() > 0)
            canvas.drawBitmap(fractalBitmap, matrix, new Paint());

        // Brings little view to front if it's hidden but shouldn't be, as can happen.
        //if (parentActivity.showingLittle) parentActivity.addLittleView(false);
    }


    /*-----------------------------------------------------------------------------------*/
    /* Render scheduling/tracking */
    /*-----------------------------------------------------------------------------------*/
    /* Adds renders to queues for each thread */
    void scheduleNewRenders() {
        // Abort current and future renders
        stopAllRendering();

        // New render won't have passed maximum depth, reset check
        hasPassedMaxDepth = false;

        // Try showing the progress spinner (won't show if, as normal, it's disallowed)
//        if (fractalViewSize == FractalViewSize.LARGE)
        parentActivity.showProgressSpinner();

        // Reset all the tracking
        for (int i = 0; i < noOfThreads; i++) {
            rendersComplete.set(i, false);
        }

        renderStartTime = System.currentTimeMillis();

        //Schedule a crude rendering if needed (not the small view, not a small zoom)

        if (SettingsActivity.performCrude(getContext()) && this.strategy.shouldPerformCrudeFirst() &&
                (totalScaleFactor < 0.6f || totalScaleFactor == 1.0f || totalScaleFactor > 3.5f || !completedLastRender)) {
            scheduleRendering(FractalPresenter.CRUDE_PIXEL_BLOCK);
        }
        totalScaleFactor = 1.0f; // Needs reset once checked, so that next render doesn't account for it.
        completedLastRender = false;

        // Schedule a high-quality rendering
        scheduleRendering(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }

    /* Add a rendering of a particular pixel size (crude or detailed) to the queues */
    void scheduleRendering(int pixelBlockSize) {
        for (int i = 0; i < noOfThreads; i++) {
            renderThreadList.get(i).allowRendering();
            renderQueueList.get(i).add(new Rendering(pixelBlockSize));
        }
    }

    /* Stop all rendering, including planned and current */
    void stopAllRendering() {
        for (int i = 0; i < noOfThreads; i++) {
            renderQueueList.get(i).clear();
            renderThreadList.get(i).abortRendering();
        }
    }

    /* Check if any threads are still rendering (returns true if so) */
    public boolean isRendering() {
        boolean allComplete = true;

        for (Boolean complete : rendersComplete) {
            if (!complete)
                return false;
        }

        return true;
    }

    /* Mark a thread as having completed a render (called in computePixels())
     * Does nothing if it's a crude render that finished. */
    public void notifyCompleteRender(int threadID, int pixelBlockSize) {
        // If detailed render has finished, note that thread has completed.
        if (pixelBlockSize == FractalPresenter.DEFAULT_PIXEL_SIZE) {
            rendersComplete.set(threadID, true);
        }

        // If all threads are done and you're the main view, show time.
        if (!(isRendering()) && strategy.shouldPerformCrudeFirst()) {
            completedLastRender = true;

            //Show time in seconds
            double time = (double) ((System.currentTimeMillis() - renderStartTime)) / 1000;
            String renderCompleteMessage = "Rendering time: " + new DecimalFormat("#.##").format(time) + " second" + (time == 1d ? "." : "s.");
            //Log.d(TAG, renderCompleteMessage);

            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("SHOW_TIMES", true))
                parentActivity.showToastOnUIThread(renderCompleteMessage, Toast.LENGTH_SHORT);

            parentActivity.hideProgressSpinner();
        }
    }

    /*-----------------------------------------------------------------------------------*/
    /* Movement */
    /*-----------------------------------------------------------------------------------*/
    /* Set new location after movement */
    public void moveFractal(int dragDiffPixelsX, int dragDiffPixelsY) {
        //Log.d(TAG, "moveFractal()");

        // What does each pixel correspond to, on the complex plane?
        double pixelSize = getPixelSize();

        // Adjust the Graph Area
        double[] newGraphArea = graphArea;
        newGraphArea[0] -= (dragDiffPixelsX * pixelSize);
        newGraphArea[1] -= -(dragDiffPixelsY * pixelSize);
        setGraphArea(newGraphArea, false);
    }

    /* Start a dragging motion - clear all the variables, stop any rendering */
    // TOUCH
    public void startDragging() {
        controlmode = ControlMode.DRAGGING;

        //Stop current rendering (to not render areas that are offscreen afterwards)
        stopAllRendering();

        //Clear translation variables
        bitmapX = 0;
        bitmapY = 0;
        totalDragX = 0;
        totalDragY = 0;

        hasZoomed = false;
    }

    /* Update the position of the image on screen as dragging happens */
    // TOUCH, REDRAW
    public void dragFractal(float dragDiffPixelsX, float dragDiffPixelsY) {
        bitmapX = dragDiffPixelsX;
        bitmapY = dragDiffPixelsY;

        totalDragX += dragDiffPixelsX;
        totalDragY += dragDiffPixelsY;

        invalidate();
    }

    /* End of a dragging motion. Move the fractal position to match the image. */
    // TOUCH, REDRAW
    public void stopDragging(boolean stoppedOnZoom) {
        controlmode = ControlMode.STATIC;

        // If no zooming's occured, keep the remaining pixels
        if (!hasZoomed && !stoppedOnZoom) {
            shiftPixels((int) totalDragX, (int) totalDragY);
        }

        //Set the new location for the fractals
        moveFractal((int) totalDragX, (int) totalDragY);

        if (!stoppedOnZoom) setGraphArea(graphArea, true);//scheduleNewRenders();

        // Reset all the variables (possibly paranoid)
        if (!hasZoomed && !stoppedOnZoom)
            matrix.reset();

        hasZoomed = false;

        invalidate();
    }

    /* Shift values in pixel array to keep pixels that have already been calculated */
    // PIXEL BUFFER ALTERATION
    public void shiftPixels(int shiftX, int shiftY) {
//        strategy.translateFractal(shiftX, shiftY);
//        this.fractalPixels = strategy.getFractalResult();
//        this.pixelSizes = strategy.getPixelSizes();
    }

    /*-----------------------------------------------------------------------------------*/
    /* Zooming */
    /*-----------------------------------------------------------------------------------*/
	/* Adjust zoom, centred on pixel (xPixel, yPixel) */
    // GRAPH AREA ALTERATION
    public void zoomChange(int xPixel, int yPixel, float scale) {
        stopAllRendering();

        double pixelSize = getPixelSize();

        double[] oldGraphArea = graphArea;
        double[] newGraphArea = new double[3];

        double zoomPercentChange = (double) scale; //= (double)(100 + (zoomAmount)) / 100;

        // What is the zoom centre?
        double zoomCentreX = oldGraphArea[0] + ((double) xPixel * pixelSize);
        double zoomCentreY = oldGraphArea[1] - ((double) yPixel * pixelSize);

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

        setGraphArea(newGraphArea, false);
    }

    /* Start a zooming gesture */
    // TOUCH
    public void startZooming(float initialMidX, float initialMidY) {
        controlmode = ControlMode.ZOOMING;
        hasZoomed = true;
        clearPixelSizes();
    }

    /* Updates zoom level during a scale gesture, but doesn't re-render */
    public void zoomImage(float focusX, float focusY, float newScaleFactor) {
        midX = focusX;
        midY = focusY;
        scaleFactor = newScaleFactor;
        totalScaleFactor *= newScaleFactor;

        // Change zoom, but don't re-render
        zoomChange((int) focusX, (int) focusY, 1 / newScaleFactor);

        invalidate();
    }

    /* End a zooming gesture (crop bitmap to screen, re-render) */
    // TOUCH, PIXEL SIZES, REDRAW, MATRIX RESET
    public void stopZooming() {
        clearPixelSizes();

        //Log.d(TAG, "Total scale factor = " + totalScaleFactor);

        controlmode = ControlMode.DRAGGING;

        stopAllRendering();

        drawPin = false;
        setDrawingCacheEnabled(true);
        fractalBitmap = Bitmap.createBitmap(getDrawingCache());
        fractalBitmap.getPixels(fractalPixels, 0, getWidth(), 0, 0, getWidth(), getHeight());
        setDrawingCacheEnabled(false);
        drawPin = true;

        bitmapX = 0;
        bitmapY = 0;
        totalDragX = 0;
        totalDragY = 0;
        matrix.reset();
    }

    /* Returns zoom level, in range 0..ZOOM_SLIDER_SCALING	(logarithmic scale) */
    public int getZoomLevel() {
        double pixelSize = getPixelSize();

        // If the pixel size = 0, something's wrong (happens at Julia launch).
        if (pixelSize == 0.0d)
            return 1;

        double lnPixelSize = Math.log(pixelSize);
        double zoomLevel = (double) ZOOM_SLIDER_SCALING * (lnPixelSize - MINZOOM_LN_PIXEL) / (MAXZOOM_LN_PIXEL - MINZOOM_LN_PIXEL);
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

    /*-----------------------------------------------------------------------------------*/
    /* Graph area */
    /*-----------------------------------------------------------------------------------*/
	/* Set a new graph area, if valid */
    // GRAPH AREA
    void setGraphArea(double[] newGraphArea, boolean newRender) {

        double[] initialGraphArea = graphArea;
        graphArea = newGraphArea;

        // Check for sane zoom level.
        if (saneZoomLevel()) {
            if (newRender) {
                if (hasPassedMaxDepth) {
                    parentActivity.showToastOnUIThread("Maximum zoom depth reached.", Toast.LENGTH_SHORT);
                }
                scheduleNewRenders();
            }
        } else {
            // Zoom level is out of bounds, just roll back.
            hasPassedMaxDepth = true;
            graphArea = initialGraphArea;
        }

    }

    /* Compute length of 1 pixel on the complex plane */
    double getPixelSize() {
        // Nothing to do - cannot compute a sane pixel size
        if (getWidth() == 0) return 0.0;
        if (graphArea == null) return 0.0;

        // Return the pixel size
        return (graphArea[2] / (double) getWidth());
    }


    /* Reset the fractal to the home graph area */
    public void canvasHome() {
        stopAllRendering();
        clearPixelSizes();

        // Default max iterations scaling
        //iterationScaling = ITERATIONSCALING_DEFAULT;

        // Default graph area
        if (parentActivity.fractalType == FractalTypeEnum.MANDELBROT) {
            setGraphArea(new MandelbrotJuliaLocation().defaultMandelbrotGraphArea, true);
        } else
            setGraphArea(new MandelbrotJuliaLocation().defaultJuliaGraphArea, true);
    }

    /*-----------------------------------------------------------------------------------*/
    /* File saving */
    /*-----------------------------------------------------------------------------------*/
	/* Saves the current fractal image as an image file 
	 * (Call in FractalActivity should ensure that image is done rendering) */
    public File saveImage() {
        //Check if external storage is available
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String filename = getNewFileName();
            File imagefile = new File(path, "FractalImage" + filename + ".png");

            //Check if it exists, try extending the names a few times if it does
            int nameTries = 0;
            while (imagefile.exists()) {
                nameTries++;
                filename += "a";
                imagefile = new File(path, "FractalImage" + filename + ".png");
                if (nameTries > 1)
                    return null;
            }

            try {
                //Check pictures directory already exists
                path.mkdir();

                //Open file output stream
                FileOutputStream output = new FileOutputStream(imagefile);
				
				/*Recreate the bitmap - all the render thread completion guarantees is that the arrays
				are full. onDraw() may not have run before saving.*/
                fractalBitmap = Bitmap.createBitmap(fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
                fractalBitmap.compress(Bitmap.CompressFormat.PNG, 90, output);

                output.close();
                //Log.d(TAG, "Wrote image out to " + imagefile.getAbsolutePath());
            } catch (IOException ioe) {
                //File writing failed
                //Log.d(TAG, "Could not write image file to " + imagefile.getAbsolutePath());
                parentActivity.showToastOnUIThread("Unable to write file.", Toast.LENGTH_LONG);
            }

            return imagefile;
        } else {
            parentActivity.showToastOnUIThread("Unable to write file.", Toast.LENGTH_LONG);
            return null;
        }
    }

    /* Generates a new filename (currently uses time down to the second) */
    private String getNewFileName() {
        String datetime = "";

        Calendar currentTime = Calendar.getInstance();
        datetime += currentTime.get(Calendar.YEAR) + "-";
        datetime += (currentTime.get(Calendar.MONTH) + 1) + "-";    //Add 1 because months start from 0, apparently.
        datetime += currentTime.get(Calendar.DAY_OF_MONTH) + "-";
        datetime += currentTime.get(Calendar.HOUR_OF_DAY) + "-";
        datetime += currentTime.get(Calendar.MINUTE) + "-";
        datetime += currentTime.get(Calendar.SECOND);

        return datetime;
    }

    /*-----------------------------------------------------------------------------------*/
    /* Thread handling  */
    /*-----------------------------------------------------------------------------------*/
	/* Interrupt (and end) the rendering threads, called when activity closing */
    public void interruptThreads() {
        for (RenderThread thread : renderThreadList) {
            thread.interrupt();
        }
    }

    /* Retrieve and remove the next rendering from a queue (used by render threads) */
    public Rendering getNextRendering(int threadID) throws InterruptedException {
        return renderQueueList.get(threadID).take();
    }

    /*-----------------------------------------------------------------------------------*/
    /* Utilities (miscellaneous useful functions)  */
    /*-----------------------------------------------------------------------------------*/
	/* Clear the sizes array of its current values, so anything new is smaller
	 * (Fills it with 1000s) */
    protected void clearPixelSizes() {
        for (int i = 0; i < pixelSizes.length; i++) {
            pixelSizes[i] = 1000;
        }

//        strategy.clearPixelSizes();
    }

    /* Stop any rendering and return to "home" position */
    public void reset() {
        stopAllRendering();

        bitmapCreations = 0;

        matrix.reset();
        fractalPixels = new int[getWidth() * getHeight()];
        clearPixelSizes();
        canvasHome();

        //postInvalidate();
    }

    /* Sets to a predetermined spot that takes a while to render (just used for debugging) */
    public void setToTestLocation() {
        stopAllRendering();

        clearPixelSizes();

        double[] bookmark = new double[3];

        bookmark[0] = -1.631509065569354;
        bookmark[1] = 0.0008548063308817164;
        bookmark[2] = 0.0027763525271276013;

        setGraphArea(bookmark, true);
    }

    /* Reloads the current location if it needs to be re-rendered
     * (After detail or colouring change) */
    public void reloadCurrentLocation() {
        stopAllRendering();

        clearPixelSizes();
        setGraphArea(graphArea, true);
    }

    /* Change the colouring scheme */
    public void setColouringScheme(String newScheme, boolean reload) {
        if (newScheme.equals("MandelbrotDefault"))
            colourer = new DefaultColourStrategy();
        else if (newScheme.equals("JuliaDefault"))
            colourer = new JuliaColourStrategy();
        else if (newScheme.equals("RGBWalk"))
            colourer = new RGBWalkColourStrategy();
        else if (newScheme.equals("Psychadelic"))
            colourer = new PsychadelicColourStrategy();

        if (reload)
            reloadCurrentLocation();
    }

    /*-----------------------------------------------------------------------------------*/
    /* Abstract methods */
    /*-----------------------------------------------------------------------------------*/
    abstract void loadLocation(MandelbrotJuliaLocation mjLocation);

    // IFractalComputeDelegate

    @Override
    public void postUpdate(int[] pixels, int[] pixelSizes) {
        this.fractalPixels = pixels;
        this.pixelSizes = pixelSizes;
        this.postInvalidate();
    }

    @Override
    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize) {
        Log.i("AFV", "Render finished for thread " + this.renderThreadList.get(0).getName());
        this.postUpdate(pixels, pixelSizes);

        this.notifyCompleteRender(0, pixelBlockSize);
    }
}


