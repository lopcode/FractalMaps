package uk.ac.ed.inf.mandelbrotmaps.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import uk.ac.ed.inf.mandelbrotmaps.overlay.IFractalOverlay;
import uk.ac.ed.inf.mandelbrotmaps.touch.IFractalTouchHandler;

public class FractalView extends View implements IFractalView {
    private final Logger LOGGER = LoggerFactory.getLogger(FractalView.class);

    private IViewResizeListener resizeListener;
    private Matrix fractalTransformMatrix;

    private int width;
    private int height;

    private Bitmap fractalBitmap;
    private Paint fractalPaint;

    private List<IFractalOverlay> presenterOverlays;
    private List<IFractalOverlay> sceneOverlays;

    private boolean drawOverlays = true;

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
        this.drawOverlays = false;
        setDrawingCacheEnabled(true);
        getDrawingCache().getPixels(pixelBuffer, 0, this.width, 0, 0, this.width, this.height);
        this.setBitmapPixels(pixelBuffer);
        setDrawingCacheEnabled(false);
        this.drawOverlays = true;
    }

    @Override
    public void setTouchHandler(IFractalTouchHandler handler) {
        this.setOnTouchListener(handler);
        this.setOnLongClickListener(handler);
    }

    @Override
    public void setSceneOverlays(List<IFractalOverlay> overlays) {
        this.sceneOverlays = overlays;
    }

    @Override
    public void setPresenterOverlays(List<IFractalOverlay> overlays) {
        this.presenterOverlays = overlays;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.width = width;
        this.height = height;

        if (this.resizeListener != null) {
            LOGGER.info("Firing onViewResized {} {}", width, height);
            this.resizeListener.onViewResized(this, width, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.width <= 0 || this.height <= 0 || this.fractalBitmap == null)
            return;

        if (this.fractalBitmap.isRecycled())
            return;

        canvas.drawBitmap(this.fractalBitmap, this.fractalTransformMatrix, this.fractalPaint);

        if (!drawOverlays)
            return;

        if (this.presenterOverlays != null) {
            for (IFractalOverlay overlay : this.presenterOverlays) {
                this.drawOverlayWithTransformedPosition(overlay, canvas);
            }
        }

        if (this.sceneOverlays != null) {
            for (IFractalOverlay overlay : this.sceneOverlays) {
                this.drawOverlayWithTransformedPosition(overlay, canvas);
            }
        }
    }

    private void drawOverlayWithTransformedPosition(IFractalOverlay overlay, Canvas canvas) {
        float[] transformedPoints = new float[2];
        this.fractalTransformMatrix.mapPoints(transformedPoints, new float[]{overlay.getX(), overlay.getY()});
        overlay.drawToCanvas(canvas, transformedPoints[0], transformedPoints[1]);
    }

    // IFractalView

    @Override
    public void postUIThreadRedraw() {
        this.invalidate();
    }

    @Override
    public void postThreadSafeRedraw() {
        this.postInvalidate();
    }

    @Override
    public void tearDown() {
        if (this.fractalBitmap != null && !this.fractalBitmap.isRecycled()) {
            this.fractalBitmap.recycle();
        }
    }
}