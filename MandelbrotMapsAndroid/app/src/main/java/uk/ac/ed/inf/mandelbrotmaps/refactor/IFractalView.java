package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.graphics.Matrix;

public interface IFractalView {
    public void redraw();

    public void setResizeListener(IViewResizeListener resizeListener);

    public void setFractalTransformMatrix(Matrix fractalTransformMatrix);

    public void createNewFractalBitmap(int[] pixels);

    public void cacheCurrentBitmap(int[] pixelBuffer);
}
