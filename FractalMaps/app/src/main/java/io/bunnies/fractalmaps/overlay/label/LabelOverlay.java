package io.bunnies.fractalmaps.overlay.label;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import io.bunnies.fractalmaps.R;
import io.bunnies.fractalmaps.overlay.IFractalOverlay;

public class LabelOverlay implements IFractalOverlay {
    private float x;
    private float y;
    private String text;
    private Paint textPaint;

    private static final float TEXT_SIZE = 32.0f;

    public LabelOverlay(Context context, String text, float initialX, float initialY) {
        this.setText(text);
        this.setPosition(initialX, initialY);
        this.textPaint = new Paint();
        this.textPaint.setColor(context.getResources().getColor(R.color.dark_red));
        this.textPaint.setTextSize(TEXT_SIZE);
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setTextAlignment(Paint.Align textAlignment) {
        this.textPaint.setTextAlign(textAlignment);
    }

    // IFractalOverlay

    @Override
    public void drawToCanvas(Canvas canvas, float drawX, float drawY) {
        canvas.drawText(this.text, drawX, drawY, this.textPaint);
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

    @Override
    public boolean isAffectedByTransform() {
        return false;
    }
}
