package uk.ac.ed.inf.mandelbrotmaps.refactor.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

public class PinOverlay implements IFractalOverlay {
    private Paint pinOuterPaint;
    private Paint pinInnerPaint;
    private float pinRadius;
    private float x;
    private float y;

    public PinOverlay(Context context, int pinOuterColour, int pinInnerColour, float pinRadius, float initialX, float initialY) {
        this.pinOuterPaint = new Paint();
        this.pinOuterPaint.setColor(context.getResources().getColor(pinOuterColour));
        this.pinOuterPaint.setAlpha(150);
        this.pinInnerPaint = new Paint();
        this.pinInnerPaint.setColor(context.getResources().getColor(pinInnerColour));
        this.pinInnerPaint.setAlpha(75);

        this.pinRadius = pinRadius;
        this.x = initialX;
        this.y = initialY;
    }

    @Override
    public void drawToCanvas(Canvas canvas, float drawX, float drawY) {
        canvas.drawCircle(drawX, drawY, this.pinRadius, this.pinOuterPaint);
        canvas.drawCircle(drawX, drawY, this.pinRadius * (0.5f), this.pinInnerPaint);
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }
}
