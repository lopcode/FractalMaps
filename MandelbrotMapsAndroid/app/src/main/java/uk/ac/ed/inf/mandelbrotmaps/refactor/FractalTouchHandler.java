package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class FractalTouchHandler implements IFractalTouchHandler {
    private IFractalTouchDelegate delegate;
    private Context context;

    // Dragging/scaling control variables
    private float dragLastX;
    private float dragLastY;
    private int dragID = -1;
    private boolean currentlyDragging = false;

    private ScaleGestureDetector gestureDetector;

    private float totalDragX = 0;
    private float totalDragY = 0;

    public FractalTouchHandler(Context context, IFractalTouchDelegate delegate) {
        this.setTouchDelegate(delegate);
        this.context = context;

        gestureDetector = new ScaleGestureDetector(this.context, this);
    }

    @Override
    public void setTouchDelegate(IFractalTouchDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        gestureDetector.onTouchEvent(evt);

        switch (evt.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.i("FA", "Touch down");
                startDragging(evt);

                return false;


            case MotionEvent.ACTION_MOVE:
                //Log.i("FA", "Touch moved");
                if (!gestureDetector.isInProgress()) {
                    if (currentlyDragging) {
                        dragFractal(evt);
                    }
                }

                return true;


            case MotionEvent.ACTION_POINTER_UP:
                if (evt.getPointerCount() == 1)
                    break;
                else {
                    try {
                        chooseNewActivePointer(evt);
                    } catch (IllegalArgumentException iae) {
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                Log.i("FA", "Touch removed");
                if (currentlyDragging) {
                    stopDragging();
                }
                break;
        }
        return false;
    }

    private void startDragging(MotionEvent evt) {
        this.totalDragX = 0;
        this.totalDragY = 0;

        dragLastX = (int) evt.getX();
        dragLastY = (int) evt.getY();
        dragID = evt.getPointerId(0);

        this.delegate.startDragging();
        currentlyDragging = true;
    }

    private void dragFractal(MotionEvent evt) {
        int pointerIndex = evt.findPointerIndex(dragID);

        float dragDiffPixelsX;
        float dragDiffPixelsY;

        try {
            dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
            dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;
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
        dragLastX = evt.getX(pointerIndex);
        dragLastY = evt.getY(pointerIndex);
    }

    private void stopDragging() {
        currentlyDragging = false;
        this.delegate.stopDragging(false, this.totalDragX, this.totalDragY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.delegate.stopDragging(true, this.totalDragX, this.totalDragY);
        this.delegate.startScaling(detector.getFocusX(), detector.getFocusY());

        currentlyDragging = false;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        this.delegate.scaleFractal(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        this.totalDragX = 0;
        this.totalDragY = 0;

        this.delegate.stopScaling();
        currentlyDragging = true;

        this.delegate.startDragging();
    }

    /* Detect a long click, place the Julia pin */
    public boolean onLongClick(View v) {
        // Check that it's not scaling, dragging (check for dragging is a little hacky, but seems to work), or already holding the pin
//        if (!gestureDetector.isInProgress() && this.totalDragX < 1 && this.totalDragY < 1 && !fractalView.holdingPin) {
//            updateLittleJulia(dragLastX, dragLastY);
//            if (currentlyDragging) {
//                stopDragging();
//            }
//            return true;
//        }

        if (!gestureDetector.isInProgress() && this.totalDragX < 1 && this.totalDragY < 1) {
            Log.i("FTH", "Long tap at " + this.dragLastX + " " + this.dragLastY);
            this.delegate.onLongClick(this.dragLastX, this.dragLastY);
            return true;
        }

        return false;
    }

    /* Choose a new active pointer from the available ones
  * Used during/at the end of scaling to pick the new dragging pointer*/
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
