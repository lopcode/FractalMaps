package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.graphics.Matrix;

public interface IFractalPresenter {
    public int[] getPixelBuffer();

    public void translatePixelBuffer(int dx, int dy);

    public void recomputeGraph(int pixelBlockSize);

    public void notifyRecomputeComplete(int pixelBlockSize);

    public int getMaxIterations();

    public double getPixelSize();

    public void setFractalDetail(double detail);

    public void setView(IFractalView view, Matrix matrix, IViewResizeListener listener);

    // Graph area affecting

    public void translateGraphArea(int dx, int dy);

    public void zoomGraphArea(int x, int y, double scale);

    public void setGraphArea(double[] graphArea);
}
