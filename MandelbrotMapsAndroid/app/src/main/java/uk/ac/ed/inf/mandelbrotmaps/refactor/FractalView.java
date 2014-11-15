package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class FractalView extends View implements IFractalComputeDelegate, IFractalTouchDelegate {
    IFractalComputeStrategy strategy;
    IFractalTouchHandler touchHandler;

    private int width;
    private int height;

    private Bitmap fractalBitmap;
    private Paint fractalPaint;
    private Matrix fractalTransformMatrix;

    public FractalView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.initialise();
    }

    public void initialise() {
        this.fractalPaint = new Paint();
        this.fractalTransformMatrix = new Matrix();
    }

    public void setFractalStrategy(IFractalComputeStrategy strategy) {
        this.strategy = strategy;
    }

    public void setTouchHandler(IFractalTouchHandler touchHandler) {
        this.touchHandler = touchHandler;
        this.touchHandler.setViewDelegate(this);

        this.setOnTouchListener(this.touchHandler);
        this.setOnLongClickListener(this.touchHandler);
        this.setLongClickable(true);
    }

    public void createNewFractalBitmap(int[] pixels) {
        this.fractalBitmap = Bitmap.createBitmap(pixels, 0, this.width, this.width, this.height, Bitmap.Config.RGB_565);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.width = width;
        this.height = height;
    }

    public void translateTransformMatrix(int x, int y) {
        this.fractalTransformMatrix.postTranslate(x, y);
    }

    public void scaleTransformMatrix(float scaleFactor, float midX, float midY) {
        this.fractalTransformMatrix.postScale(scaleFactor, scaleFactor, midX, midY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.width <= 0 || this.height <= 0 || this.fractalBitmap == null)
            return;

        //Draw fractal image on screen
        canvas.drawBitmap(this.fractalBitmap, this.fractalTransformMatrix, this.fractalPaint);
    }

    // IFractalComputeDelegate

    @Override
    public void postUpdate(int[] pixels, int[] pixelSizes) {
        this.createNewFractalBitmap(pixels);
        this.postInvalidate();
    }

    @Override
    public void postFinished(int[] pixels, int[] pixelSizes, int pixelBlockSize) {

    }

    // IFractalTouchDelegate

    @Override
    public void dragFractal(int x, int y) {
        this.translateTransformMatrix(x, y);
        this.postInvalidate();
    }

    @Override
    public void scaleFractal(float scaleFactor, float midX, float midY) {
        this.scaleTransformMatrix(scaleFactor, midX, midY);
        this.postInvalidate();
    }
}
