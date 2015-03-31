package uk.ac.ed.inf.mandelbrotmaps.touch;

import android.content.Context;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.IPinMovementDelegate;

public class MandelbrotTouchHandler extends FractalTouchHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(MandelbrotTouchHandler.class);

    private IPinMovementDelegate delegate;
    private boolean draggingPin;

    public MandelbrotTouchHandler(Context context, IFractalTouchDelegate delegate) {
        super(context, delegate);
        this.draggingPin = false;
    }

    public void setPinMovementDelegate(IPinMovementDelegate delegate) {
        this.delegate = delegate;
    }

    public boolean isTouchingPin(float x, float y) {
        float pinRadius = this.delegate.getPinRadius();
        float pinX = this.delegate.getPinX();
        float pinY = this.delegate.getPinY();

        if (x > (pinX - pinRadius) && x < (pinX + pinRadius))
            if (y > (pinY - pinRadius) && y < (pinY + pinRadius))
                return true;

        return false;
    }

    @Override
    protected boolean onTouchDown(float x, float y, int pointerID, int pointerCount) {
        if (this.draggingPin && pointerCount > 1) {
            this.stopDraggingPin(this.delegate.getPinX(), this.delegate.getPinY());
        }

        if (isTouchingPin(x, y) && pointerCount == 1) {
            LOGGER.debug("Started dragging pin");
            this.draggingPin = true;
            this.delegate.startedDraggingPin();

            this.dragID = pointerID;

            return false;
        }

        return super.onTouchDown(x, y, pointerID, pointerCount);
    }

    @Override
    protected boolean onTouchMove(float x, float y, int pointerCount) {
        if (this.draggingPin && pointerCount > 1) {
            this.stopDraggingPin(this.delegate.getPinX(), this.delegate.getPinY());
        }

        if (this.draggingPin && pointerCount == 1) {
            //Log.i("MTH", "Dragged pin");
            this.delegate.pinDragged(x, y, false);

            return true;
        }

        return super.onTouchMove(x, y, pointerCount);
    }

    @Override
    protected boolean onTouchUp(float x, float y) {
        LOGGER.debug("Touch removed");
        if (this.draggingPin) {
            this.stopDraggingPin(x, y);
            return false;
        }

        return super.onTouchUp(x, y);
    }

    private void stopDraggingPin(float x, float y) {
        LOGGER.debug("Stopped dragging pin");
        this.draggingPin = false;
        this.delegate.stoppedDraggingPin(x, y);
    }

    @Override
    public boolean onLongClick(View v) {
        if (!this.draggingPin) {
            return super.onLongClick(v);
        }

        return false;
    }
}
