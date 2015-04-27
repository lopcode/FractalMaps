package io.bunnies.fractalmaps.overlay;

import android.graphics.Canvas;

public interface IFractalOverlay {
    public void drawToCanvas(Canvas canvas, float drawX, float drawY);

    public void setPosition(float x, float y);

    public float getX();

    public float getY();

    public boolean isAffectedByTransform();
}
