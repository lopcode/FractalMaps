package uk.ac.ed.inf.mandelbrotmaps.compute;

public class FractalComputeArguments {
    public final int pixelBlockSize;
    public final int maxIterations;
    public final int linesPerProgressUpdate;
    public final int defaultPixelSize;
    public final int viewWidth;
    public final int viewHeight;
    public final double xMin;
    public final double yMax;
    public final double pixelSize;
    public int[] pixelBuffer;
    public int[] pixelBufferSizes;
    public long startTime;
    public boolean killed = false;

    public FractalComputeArguments(int pixelBlockSize,
                                   final int maxIterations,
                                   final int linesPerProgressUpdate,
                                   final int defaultPixelSize,
                                   final int viewWidth,
                                   final int viewHeight,
                                   final double xMin,
                                   final double yMax,
                                   final double pixelSize,
                                   int[] pixelBuffer,
                                   int[] pixelBufferSizes) {
        this.pixelBlockSize = pixelBlockSize;
        this.maxIterations = maxIterations;
        this.linesPerProgressUpdate = linesPerProgressUpdate;
        this.defaultPixelSize = defaultPixelSize;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.xMin = xMin;
        this.yMax = yMax;
        this.pixelSize = pixelSize;
        this.pixelBuffer = pixelBuffer;
        this.pixelBufferSizes = pixelBufferSizes;
    }
}
