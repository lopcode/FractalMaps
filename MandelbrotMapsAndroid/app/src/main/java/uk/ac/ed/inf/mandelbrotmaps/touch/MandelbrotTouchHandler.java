package uk.ac.ed.inf.mandelbrotmaps.touch;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.IPinMovementDelegate;

public class MandelbrotTouchHandler extends FractalTouchHandler {
    private IPinMovementDelegate delegate;
    private boolean draggingPin;

    public MandelbrotTouchHandler(Context context, IFractalTouchDelegate delegate) {
        super(context, delegate);
        this.draggingPin = false;
    }

    public void setPinMovementDelegate(IPinMovementDelegate delegate) {
        this.delegate = delegate;
    }

    private boolean isTouchingPin(float x, float y) {
        float pinRadius = this.delegate.getPinRadius();
        float pinX = this.delegate.getPinX();
        float pinY = this.delegate.getPinY();

        if (x > (pinX - pinRadius) && x < (pinX + pinRadius))
            if (y > (pinY - pinRadius) && y < (pinY + pinRadius))
                return true;

        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        boolean stopDraggingPin = false;

        switch (evt.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.i("MTH", "Touch down");

                if (isTouchingPin(evt.getX(), evt.getY()) && evt.getPointerCount() == 1) {
                    Log.i("MTH", "Started dragging pin");
                    this.draggingPin = true;
                    this.delegate.startedDraggingPin();
                    return true;
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (this.draggingPin && evt.getPointerCount() == 1) {
                    //Log.i("MTH", "Dragged pin");
                    this.delegate.pinDragged(evt.getX(), evt.getY(), false);
                    return true;
                }

                break;

            case MotionEvent.ACTION_UP:
                Log.i("MTH", "Touch removed");
                if (this.draggingPin) {
                    Log.i("MTH", "Stopped dragging pin");
                    this.draggingPin = false;
                    stopDraggingPin = true;
                    this.delegate.stoppedDraggingPin(evt.getX(), evt.getY());

                    return true;
                }

                break;
        }

        if (!draggingPin && !stopDraggingPin) {
            return super.onTouch(v, evt);
        }

        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (!this.draggingPin) {
            return super.onLongClick(v);
        }

        return false;
    }
}
