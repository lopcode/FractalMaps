package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class FractalView extends View implements IFractalView {
    private IViewResizeListener resizeListener;
    private Matrix fractalTransformMatrix;

    private int width;
    private int height;

    private Bitmap fractalBitmap;
    private Paint fractalPaint;

    public FractalView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.initialise();
    }

    public void initialise() {
        this.fractalPaint = new Paint();

        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
    }

    public void setResizeListener(IViewResizeListener resizeListener) {
        this.resizeListener = resizeListener;
    }

    public void setFractalTransformMatrix(Matrix fractalTransformMatrix) {
        this.fractalTransformMatrix = fractalTransformMatrix;
    }

    public void createNewFractalBitmap(int[] pixels) {
        this.fractalBitmap = Bitmap.createBitmap(pixels, 0, this.width, this.width, this.height, Bitmap.Config.RGB_565).copy(Bitmap.Config.RGB_565, true);
    }

    public void setBitmapPixels(int[] pixels) {
        this.fractalBitmap.setPixels(pixels, 0, this.width, 0, 0, this.width, this.height);
    }

    @Override
    public void cacheCurrentBitmap(int[] pixelBuffer) {
        setDrawingCacheEnabled(true);
        getDrawingCache().getPixels(pixelBuffer, 0, this.width, 0, 0, this.width, this.height);
        this.setBitmapPixels(pixelBuffer);
        setDrawingCacheEnabled(false);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.width = width;
        this.height = height;

        if (this.resizeListener != null)
            this.resizeListener.onViewResized(this, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.width <= 0 || this.height <= 0 || this.fractalBitmap == null)
            return;

        canvas.drawBitmap(this.fractalBitmap, this.fractalTransformMatrix, this.fractalPaint);
    }

    // IFractalView

    @Override
    public void redraw() {
        this.postInvalidate();
    }
}