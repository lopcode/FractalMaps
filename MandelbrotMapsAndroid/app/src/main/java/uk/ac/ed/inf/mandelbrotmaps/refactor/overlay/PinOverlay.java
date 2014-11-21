package uk.ac.ed.inf.mandelbrotmaps.refactor.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import uk.ac.ed.inf.mandelbrotmaps.R;

public class PinOverlay implements IFractalOverlay {
    private Context context;
    private Paint pinOuterPaint;
    private Paint pinInnerPaint;
    private float pinRadius;
    private float x;
    private float y;
    private boolean hilighted;

    private static final int OUTER_ALPHA = 150;
    private static final int INNER_ALPHA = 75;

    public PinOverlay(Context context, float pinRadius, float initialX, float initialY) {
        this.context = context;
        this.pinOuterPaint = new Paint();
        this.pinInnerPaint = new Paint();
        this.pinRadius = pinRadius;
        this.x = initialX;
        this.y = initialY;
    }

    @Override
    public void drawToCanvas(Canvas canvas, float drawX, float drawY) {
        float drawRadius = this.pinRadius;
        if (this.hilighted)
            drawRadius *= 2.0f;

        canvas.drawCircle(drawX, drawY, drawRadius, this.pinOuterPaint);
        canvas.drawCircle(drawX, drawY, drawRadius * (0.5f), this.pinInnerPaint);
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

    public float getPinRadius() {
        return this.pinRadius;
    }

    public void setHilighted(boolean hilighted) {
        this.hilighted = hilighted;
    }

    private void setPinColours(int innerPinColour, int outerPinColour) {
        this.pinOuterPaint.setColor(this.context.getResources().getColor(outerPinColour));
        this.pinOuterPaint.setAlpha(OUTER_ALPHA);
        this.pinInnerPaint.setColor(this.context.getResources().getColor(innerPinColour));
        this.pinInnerPaint.setAlpha(INNER_ALPHA);
    }

    public void setPinColour(PinColour colour) {
        switch (colour) {
            case RED:
                this.setPinColours(R.color.dark_red, R.color.red);
                break;

            case GREEN:
                this.setPinColours(R.color.dark_green, R.color.green);
                break;

            case BLUE:
                this.setPinColours(R.color.dark_blue, R.color.blue);
                break;

            case YELLOW:
                this.setPinColours(R.color.dark_yellow, R.color.yellow);
                break;

            case MAGENTA:
                this.setPinColours(R.color.dark_purple, R.color.purple);
                break;

            case BLACK:
                this.setPinColours(R.color.black, R.color.gray);
                break;
        }
    }
}
