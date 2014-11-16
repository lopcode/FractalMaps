package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalPresenter {
    public int[] getPixelBuffer();

    public void translatePixelBuffer(int dx, int dy);

    public void recomputeGraph(int pixelBlockSize);

    public int getMaxIterations();

    public double getPixelSize();

    public void setFractalDetail(double detail);

    // Graph area affecting

    public void translateGraphArea(int dx, int dy);

    public void zoomGraphArea(int x, int y, double scale);

    public void setGraphArea(double[] graphArea);
}
