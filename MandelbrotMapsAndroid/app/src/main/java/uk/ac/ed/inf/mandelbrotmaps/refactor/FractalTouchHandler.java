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

    public FractalTouchHandler(Context context, IFractalTouchDelegate delegate) {
        this.setTouchDelegate(delegate);
        this.context = context;

        gestureDetector = new ScaleGestureDetector(this.context, this);
    }

    @Override
    public void setTouchDelegate(IFractalTouchDelegate delegate) {
        this.delegate = delegate;
    }

    public boolean onTouch(View v, MotionEvent evt) {
        gestureDetector.onTouchEvent(evt);

        switch (evt.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
//                if (showingLittle && evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
//                    borderView.setBackgroundColor(Color.DKGRAY);
//                    littleFractalSelected = true;
//                } else
//                if (!gestureDetector.isInProgress()
//                        && !fractalView.holdingPin && (touchingPin(evt.getX(), evt.getY()))) {
//                    // Take hold of the pin, reset the little fractal view.
//                    fractalView.holdingPin = true;
//                    updateLittleJulia(evt.getX(), evt.getY());
//                } else {
                Log.i("FA", "Touch down");
                startDragging(evt);
                return true;
//                }


            case MotionEvent.ACTION_MOVE:
                //Log.i("FA", "Touch moved");
                if (!gestureDetector.isInProgress()) {
                    if (currentlyDragging) {
                        dragFractal(evt);
                    }
//                    else if (fractalView.holdingPin) {
//                        updateLittleJulia(evt.getX(), evt.getY());
//                    }
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
//                else if (littleFractalSelected) {
//                    borderView.setBackgroundColor(Color.GRAY);
//                    littleFractalSelected = false;
//                    if (evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
//                        if (fractalType == FractalTypeEnum.MANDELBROT) {
//                            launchJulia(((JuliaFractalView) littleFractalView).getJuliaParam());
//                        } else if (fractalType == FractalTypeEnum.JULIA) {
//                            finish();
//                        }
//                    }
//                }
                // If holding the pin, drop it, update screen (render won't display while dragging, might've finished in background)
//                else if (fractalView.holdingPin) {
//                    fractalView.holdingPin = false;
//                    updateLittleJulia(evt.getX(), evt.getY());
//                }
//
//                fractalView.holdingPin = false;

                break;
        }
        return false;
    }

    private void startDragging(MotionEvent evt) {
        dragLastX = (int) evt.getX();
        dragLastY = (int) evt.getY();
        dragID = evt.getPointerId(0);

        this.delegate.startDragging();
        currentlyDragging = true;
    }

    private void dragFractal(MotionEvent evt) {
        try {
            int pointerIndex = evt.findPointerIndex(dragID);

            float dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
            float dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;

            // Move the canvas
            if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f) {
                this.delegate.dragFractal(dragDiffPixelsX, dragDiffPixelsY);

//                // Proof of concept julia changing
//                double[] juliaSeed = juliaStrategy.getJuliaSeed();
//                Log.i("FA", "Julia seed " + juliaSeed[0] + " " + juliaSeed[1]);
//                juliaStrategy.setJuliaSeed(juliaSeed[0] + 0.01, juliaSeed[1] + 0.01);
//                this.secondFractalPresenter.clearPixelSizes();
//                this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
            }

            // Update last mouse position
            dragLastX = evt.getX(pointerIndex);
            dragLastY = evt.getY(pointerIndex);
        } catch (Exception iae) {
            // TODO: Investigate why this is in a try-catch block
        }
    }

    private void stopDragging() {
        currentlyDragging = false;
        this.delegate.stopDragging(false);
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.delegate.stopDragging(true);
        this.delegate.startScaling(detector.getFocusX(), detector.getFocusY());

        currentlyDragging = false;
        return true;
    }

    public boolean onScale(ScaleGestureDetector detector) {
        this.delegate.scaleFractal(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        this.delegate.stopScaling();
        currentlyDragging = true;

        this.delegate.startDragging();
    }

    /* Detect a long click, place the Julia pin */
    public boolean onLongClick(View v) {
        // Check that it's not scaling, dragging (check for dragging is a little hacky, but seems to work), or already holding the pin
//        if (!gestureDetector.isInProgress() && fractalView.totalDragX < 1 && fractalView.totalDragY < 1 && !fractalView.holdingPin) {
//            updateLittleJulia(dragLastX, dragLastY);
//            if (currentlyDragging) {
//                stopDragging();
//            }
//            return true;
//        }

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
