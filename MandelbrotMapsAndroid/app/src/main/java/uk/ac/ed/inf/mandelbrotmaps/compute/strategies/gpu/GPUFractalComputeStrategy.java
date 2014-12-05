package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

import android.content.Context;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.ed.inf.mandelbrotmaps.R;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;
import uk.ac.ed.inf.mandelbrotmaps.compute.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.FractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.presenter.FractalPresenter;

public abstract class GPUFractalComputeStrategy extends FractalComputeStrategy {
    private RenderScript renderScript;
    protected ScriptC_mandelbrot fractalRenderScript;
    private Allocation pixelBufferAllocation;
    private Allocation pixelBufferSizesAllocation;
    private Context context;

    private LinkedBlockingQueue<FractalComputeArguments> renderQueueList = new LinkedBlockingQueue<FractalComputeArguments>();
    private GPURenderThread renderThreadList;
    private Boolean rendersComplete;

    private Allocation row_indices_alloc;
    private SparseArray<int[][]> rowIndices;

    private int linesPerProgressUpdate;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        super.initialise(width, height, delegate);

        this.linesPerProgressUpdate = this.height / 4;

        this.initialiseRenderThread();
        this.initialiseRenderScript();
        this.initialiseRowIndexCache(Arrays.asList(new Integer[]{FractalPresenter.CRUDE_PIXEL_BLOCK, FractalPresenter.DEFAULT_PIXEL_SIZE}), linesPerProgressUpdate);
    }

    public void initialiseRowIndexCache(List<Integer> pixelBlockSizesToPrecompute, int linesPerProgressUpdate) {
        this.rowIndices = new SparseArray<int[][]>(2);
        int size = this.width * this.height;

        for (Integer pixelBlockSize : pixelBlockSizesToPrecompute) {
            ArrayList<Integer> row_indices = new ArrayList<Integer>(2000);

            int numberOfThreads = 4;
            for (int threadID = 0; threadID < numberOfThreads; threadID++) {
                int yStart = (this.height / 2) + (threadID * pixelBlockSize);
                int yEnd = this.height;

                int xPixelMin = 0;
                int xPixelMax = this.width;
                int yPixelMin = yStart;
                int yPixelMax = yEnd;

                int imgWidth = xPixelMax - xPixelMin;
                int xPixel = 0;
                int yPixel = 0;
                int yIncrement = 0;

                int pixelIncrement = pixelBlockSize * numberOfThreads;
                int originalIncrement = pixelIncrement;

                int loopCount = 0;

                for (yIncrement = yPixelMin; yPixel < yPixelMax + (numberOfThreads * pixelBlockSize); yIncrement += pixelIncrement) {
                    yPixel = yIncrement;

                    pixelIncrement = (loopCount * originalIncrement);
                    if (loopCount % 2 == 0) {
                        pixelIncrement *= -1;
                    }

                    loopCount++;

                    if (((imgWidth * (yPixel + pixelBlockSize - 1)) + xPixelMax) > size || yPixel < 0) {
                        //rsDebug("exceeded bounds of image", 0);
                        //rsDebug("yPixel", yPixel);
                        //rsDebug("pixelBufferSizesLength", arraySize);
                        continue;
                    }

                    row_indices.add(yPixel);
                }
            }

            int numRows = row_indices.size();
            int[] primRowIndices = this.buildIntArray(row_indices);
            int progressUpdates = (int) Math.ceil(numRows / (double) linesPerProgressUpdate);
            int[][] indices = new int[progressUpdates][];

            int progressUpdate = 0;
            for (int i = 0; i < numRows; i += linesPerProgressUpdate) {
                indices[progressUpdate] = Arrays.copyOfRange(primRowIndices, i, i + linesPerProgressUpdate);
                progressUpdate++;
            }

            rowIndices.put(pixelBlockSize, indices);
        }
    }

    public void initialiseRenderThread() {
        if (this.renderThreadList != null && this.renderThreadList != null) {
            this.stopAllRendering();
            this.interruptThreads();
        }


        this.rendersComplete = false;
        this.renderQueueList = new LinkedBlockingQueue<FractalComputeArguments>();
        this.renderThreadList = new GPURenderThread(this, 0);
        this.renderThreadList.start();

    }

    public void interruptThreads() {
        if (renderThreadList != null)
            renderThreadList.interrupt();
    }

    private void initialisePixelBufferAllocation(int size) {
        this.pixelBufferAllocation = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), size, Allocation.USAGE_SCRIPT);
        this.fractalRenderScript.bind_returnPixelBuffer(this.pixelBufferAllocation);
    }

    private void initialisePixelBufferSizesAllocation(int size) {
        this.pixelBufferSizesAllocation = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), size, Allocation.USAGE_SCRIPT);
        this.fractalRenderScript.bind_returnPixelBufferSizes(this.pixelBufferSizesAllocation);
    }

    private boolean initialiseRenderScript() {
        try {
            this.renderScript = RenderScript.create(this.context);
            this.fractalRenderScript = new ScriptC_mandelbrot(this.renderScript, context.getResources(), R.raw.mandelbrot);
        } catch (Throwable throwable) {
            Log.e("GFCS", "Failed to initialise renderscript: " + throwable.getLocalizedMessage());
            return false;
        }

        this.fractalRenderScript.set_gScript(this.fractalRenderScript);

        this.initialisePixelBufferAllocation(this.width * this.height);
        this.initialisePixelBufferSizesAllocation(this.width * this.height);

        Log.i("GFCS", "Initialised renderscript objects successfully");

        return true;
    }

    public void destroyRenderscriptObjects() {
        if (this.pixelBufferAllocation != null) {
            this.pixelBufferAllocation.destroy();
            this.pixelBufferAllocation = null;
        }

        if (this.pixelBufferSizesAllocation != null) {
            this.pixelBufferSizesAllocation.destroy();
            this.pixelBufferSizesAllocation = null;
        }

        if (this.fractalRenderScript != null) {
            this.fractalRenderScript.destroy();
            this.fractalRenderScript = null;
        }

        if (this.renderScript != null) {
            this.renderScript.destroy();
            this.renderScript = null;
        }
    }

    @Override
    public synchronized void tearDown() {
        this.destroyRenderscriptObjects();

        this.stopAllRendering();
        this.interruptThreads();
    }

    @Override
    public void computeFractal(FractalComputeArguments arguments) {
        this.stopAllRendering();

        rendersComplete = false;

        this.scheduleRendering(arguments);
        //Log.i("GFCS", "Scheduling renderscript render on separate thread");
    }

    private int[] buildIntArray(List<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }

    public void computeFractalWithThreadID(FractalComputeArguments arguments, int _threadID) {
        if (this.renderScript == null)
            return;

        this.delegate.onComputeStarted(arguments.pixelBlockSize);

        long setupStart = System.nanoTime();

        int size = arguments.viewHeight * arguments.viewWidth;

        if (this.pixelBufferAllocation == null || this.pixelBufferAllocation.getType().getCount() != size) {
            if (this.pixelBufferAllocation != null)
                this.pixelBufferAllocation.destroy();

            this.initialisePixelBufferAllocation(size);
        }

        if (this.pixelBufferSizesAllocation == null || this.pixelBufferSizesAllocation.getType().getCount() != size) {
            if (this.pixelBufferSizesAllocation != null)
                this.pixelBufferSizesAllocation.destroy();

            this.initialisePixelBufferSizesAllocation(size);
        }

        this.pixelBufferAllocation.copyFrom(arguments.pixelBuffer);
        this.pixelBufferSizesAllocation.copyFrom(arguments.pixelBufferSizes);

        //Log.i("GFCS", "Starting renderscript");
        //(int pixelBlockSize, int maxIterations, int defaultPixelSize,
        // int viewWidth, int viewHeight, double xMin, double yMax,
        // double pixelSize, int arraySize) {

        this.fractalRenderScript.set_pixelBlockSize(arguments.pixelBlockSize);
        this.fractalRenderScript.set_maxIterations(arguments.maxIterations);
        this.fractalRenderScript.set_defaultPixelSize(arguments.defaultPixelSize);
        this.fractalRenderScript.set_viewWidth(arguments.viewWidth);

        this.fractalRenderScript.set_xMin(arguments.xMin);
        this.fractalRenderScript.set_yMax(arguments.yMax);
        this.fractalRenderScript.set_pixelSize(arguments.pixelSize);
        this.fractalRenderScript.set_arraySize(size);

        if (this.row_indices_alloc == null || this.row_indices_alloc.getType().getCount() != size) {
            if (this.row_indices_alloc != null)
                this.row_indices_alloc.destroy();

            this.row_indices_alloc = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), arguments.linesPerProgressUpdate, Allocation.USAGE_SCRIPT);
            this.fractalRenderScript.set_gIn(row_indices_alloc);
            this.fractalRenderScript.set_gOut(row_indices_alloc);
        }

        int lastAllocSize = arguments.linesPerProgressUpdate;

        long setupEnd = System.nanoTime();
        double setupTime = (setupEnd - setupStart) / 1000000000D;
        //Log.i("GFCS", "Took " + setupTime + " seconds to set up for GPU compute");

        int[][] indices = this.rowIndices.get(arguments.pixelBlockSize);
        int progressUpdates = indices.length;
        for (int i = 0; i < progressUpdates; i++) {
            int linesInProgressUpdate = indices[i].length;

            if (linesInProgressUpdate != lastAllocSize) {
                row_indices_alloc.destroy();
                row_indices_alloc = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), linesInProgressUpdate, Allocation.USAGE_SCRIPT);
                this.fractalRenderScript.set_gIn(row_indices_alloc);
                this.fractalRenderScript.set_gOut(row_indices_alloc);
                lastAllocSize = linesInProgressUpdate;
                //Log.i("GFCS", "Created new allocation size");
            }

            row_indices_alloc.copyFrom(indices[i]);

            this.invokeComputeFunction();

            if (arguments.pixelBuffer != null) {
                //Log.i("GFCS", "Copying pixel buffer");
                this.pixelBufferAllocation.copyTo(arguments.pixelBuffer);
            } else {
                return;
            }

            if (arguments.pixelBufferSizes != null) {
                //Log.i("GFCS", "Copying pixel buffer sizes");
                this.pixelBufferSizesAllocation.copyTo(arguments.pixelBufferSizes);
            } else {
                return;
            }

            if (!renderThreadList.abortSignalled() && arguments.linesPerProgressUpdate != arguments.viewHeight)
                this.delegate.postUpdate(arguments.pixelBuffer, arguments.pixelBufferSizes);
        }


        //Log.i("GFCS", "Done");
//
//        for (int i = 0; i < size; i++) {
//            if (arguments.pixelBuffer[i] != 0) {
//                Log.i("GFCS", "Not zero");
//            }
//        }
        if (!renderThreadList.abortSignalled())
            this.delegate.postFinished(arguments.pixelBuffer, arguments.pixelBufferSizes, arguments.pixelBlockSize);

        long endTime = System.nanoTime();
        double allTime = (endTime - setupStart) / 1000000000D;
        Log.i("GFCS", "Took " + allTime + " seconds to do GPU compute");
    }

    void scheduleRendering(FractalComputeArguments arguments) {

        renderThreadList.allowRendering();
        renderQueueList.add(arguments);

    }

    @Override
    public boolean shouldPerformCrudeFirst() {
        return false;
    }

    @Override
    public void stopAllRendering() {
        if (!this.renderQueueList.isEmpty())
            this.renderQueueList.clear();

        this.renderThreadList.abortRendering();
    }

    public FractalComputeArguments getNextRendering(int threadID) throws InterruptedException {
        return this.renderQueueList.take();
    }

    protected abstract void invokeComputeFunction();
}
