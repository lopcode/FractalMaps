package io.bunnies.fractalmaps.presenter;

import android.graphics.Matrix;

import io.bunnies.fractalmaps.compute.strategies.IFractalComputeStrategy;
import io.bunnies.fractalmaps.touch.IFractalTouchHandler;
import io.bunnies.fractalmaps.view.IFractalView;
import io.bunnies.fractalmaps.view.IViewResizeListener;

public interface IFractalPresenter extends IFractalPresenterDelegate {
    public int[] getPixelBuffer();

    public void translatePixelBuffer(int dx, int dy);

    public void clearPixelSizes();

    public void recomputeGraph(int pixelBlockSize);

    public void notifyRecomputeComplete(int pixelBlockSize, double timeTakenInSeconds);

    public int getMaxIterations();

    public void setFractalDetail(double detail);

    public void setView(IFractalView view, Matrix matrix, IViewResizeListener listener);

    public void setTouchHandler(IFractalTouchHandler touchHandler);

    public void setComputeStrategy(IFractalComputeStrategy strategy);

    public double[] getGraphPositionFromClickedPosition(float touchX, float touchY);

    public double[] getPointFromGraphPosition(double pointX, double pointY);

    public void initialiseStrategy();

    public IFractalComputeStrategy getComputeStrategy();

    // Graph area affecting

    public void translateGraphArea(int dx, int dy);

    public void setGraphArea(double[] graphArea);

    public double[] getGraphArea();

    public void computeGraphAreaNow(double[] graphArea);
}
