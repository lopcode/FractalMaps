package uk.ac.ed.inf.mandelbrotmaps.touch;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FractalTouchHandler implements IFractalTouchHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(FractalTouchHandler.class);

    private IFractalTouchDelegate delegate;
    private Context context;

    // Dragging/scaling control variables
    private float dragLastX;
    private float dragLastY;
    protected int dragID = -1;
    private boolean currentlyDragging = false;
    private boolean currentlyScaling = false;

    private ScaleGestureDetector gestureDetector;

    private float totalDragX = 0;
    private float totalDragY = 0;
    private float currentScaleFactor = 0;

    private static final float MAX_MOVEMENT_JITTER = 5f;

    public FractalTouchHandler(Context context, IFractalTouchDelegate delegate) {
        this.setTouchDelegate(delegate);
        this.context = context;

        gestureDetector = new ScaleGestureDetector(this.context, this);
    }

    public float getTotalDragX() {
        return this.totalDragX;
    }

    public float getTotalDragY() {
        return this.totalDragY;
    }

    public int getCurrentPointerID() {
        return this.dragID;
    }

    public boolean isCurrentlyDragging() {
        return this.currentlyDragging;
    }

    public float getCurrentScaleFactor() {
        return this.currentScaleFactor;
    }

    public boolean isCurrentlyScaling() {
        return this.gestureDetector.isInProgress() || this.currentlyScaling;
    }

    @Override
    public void setTouchDelegate(IFractalTouchDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        this.gestureDetector.onTouchEvent(evt);

        switch (evt.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                return this.onTouchDown(evt.getX(), evt.getY(), evt.getPointerId(0), evt.getPointerCount());
            }

            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = evt.findPointerIndex(this.dragID);
                return this.onTouchMove(evt.getX(pointerIndex), evt.getY(pointerIndex), evt.getPointerCount());
            }

            case MotionEvent.ACTION_POINTER_UP: {
                if (evt.getPointerCount() == 1) {
                    break;
                } else {
                    try {
                        chooseNewActivePointer(evt);
                    } catch (IllegalArgumentException iae) {
                    }
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                int pointerIndex = evt.findPointerIndex(this.dragID);
                return this.onTouchUp(evt.getX(pointerIndex), evt.getY(pointerIndex));
            }
        }

        return false;
    }

    protected boolean onTouchDown(float x, float y, int pointerID, int pointerCount) {
        LOGGER.debug("Touch down");

        this.startDraggingFractal(x, y, pointerID);
        return false;
    }

    protected boolean onTouchMove(float x, float y, int pointerCount) {
        if (!this.gestureDetector.isInProgress()) {
            if (this.isCurrentlyDragging()) {
                this.dragFractal(x, y);
            }
        }

        return true;
    }

    protected boolean onTouchUp(float x, float y) {
        LOGGER.debug("Touch removed");

        if (this.isCurrentlyDragging()) {
            stopDraggingFractal();
        }

        return false;
    }

    public void startDraggingFractal(float x, float y, int pointerID) {
        this.totalDragX = 0;
        this.totalDragY = 0;

        this.dragLastX = (int) x;
        this.dragLastY = (int) y;
        this.dragID = pointerID;

        this.delegate.startDraggingFractal();
        this.currentlyDragging = true;
    }

    public void dragFractal(float x, float y) {
        float dragDiffPixelsX;
        float dragDiffPixelsY;

        try {
            dragDiffPixelsX = x - dragLastX;
            dragDiffPixelsY = y - dragLastY;
        } catch (IllegalArgumentException e) {
            return;
        }

        // Move the canvas
        if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f) {
            this.totalDragX += dragDiffPixelsX;
            this.totalDragY += dragDiffPixelsY;

            this.delegate.dragFractal(dragDiffPixelsX, dragDiffPixelsY, this.totalDragX, this.totalDragY);
        }

        // Update last mouse position
        this.dragLastX = x;
        this.dragLastY = y;
    }

    public void stopDraggingFractal() {
        currentlyDragging = false;
        this.delegate.stopDraggingFractal(false, this.totalDragX, this.totalDragY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.startScalingFractal(detector.getFocusX(), detector.getFocusY());

        return true;
    }

    public void startScalingFractal(float focusX, float focusY) {
        this.delegate.stopDraggingFractal(true, this.totalDragX, this.totalDragY);
        this.delegate.startScalingFractal(focusX, focusY);

        this.currentlyDragging = false;
        this.currentlyScaling = true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        this.scaleFractal(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());

        return true;
    }

    public void scaleFractal(float focusX, float focusY, float scaleFactor) {
        this.currentScaleFactor = scaleFactor;
        this.delegate.scaleFractal(this.currentScaleFactor, focusX, focusY);
    }

    private boolean hasNotMovedOrScaledWithJitter() {
        boolean hasStayedStill = Math.sqrt(Math.pow(this.totalDragX, 2) + Math.pow(this.totalDragY, 2)) < MAX_MOVEMENT_JITTER;
        boolean hasNotScaled = !this.isCurrentlyScaling();

        return hasStayedStill && hasNotScaled;
    }

    public boolean onLongClick(View v) {
        if (this.hasNotMovedOrScaledWithJitter()) {
            LOGGER.info("Long tap at {} {}", this.dragLastX, this.dragLastY);
            this.delegate.onLongClick(this.dragLastX, this.dragLastY);
            return true;
        }

        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        this.stopScalingFractal();
    }

    public void stopScalingFractal() {
        this.totalDragX = 0;
        this.totalDragY = 0;
        this.currentScaleFactor = 0;

        this.delegate.stopScalingFractal();
        this.currentlyScaling = false;

        this.currentlyDragging = true;
        this.delegate.startDraggingFractal();
    }

    // Choose a new active pointer, from available pointers
    // Applicable during and after scaling operations, to decide which pointer to drag with
    private void chooseNewActivePointer(MotionEvent evt) {
        // Extract the index of the pointer that came up
        final int pointerIndex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = evt.getPointerId(pointerIndex);

        //evt.getX/Y() can apparently throw these exceptions, in some versions of Android (2.2, at least)
        //(https://android-review.googlesource.com/#/c/21318/)
        try {
            dragLastX = (int) evt.getX(dragID);
            dragLastY = (int) evt.getY(dragID);

            if (pointerId == dragID) {
                //Log.d(TAG, "Choosing new active pointer");
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                dragLastX = (int) evt.getX(newPointerIndex);
                dragLastY = (int) evt.getY(newPointerIndex);
                dragID = evt.getPointerId(newPointerIndex);
            }
        } catch (ArrayIndexOutOfBoundsException aie) {
        }
    }
}
