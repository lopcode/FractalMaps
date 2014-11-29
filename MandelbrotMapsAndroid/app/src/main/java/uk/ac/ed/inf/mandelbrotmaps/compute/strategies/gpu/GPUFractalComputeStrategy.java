package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

import android.content.Context;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.ed.inf.mandelbrotmaps.R;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;
import uk.ac.ed.inf.mandelbrotmaps.compute.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.FractalComputeStrategy;

public class GPUFractalComputeStrategy extends FractalComputeStrategy {
    private RenderScript renderScript;
    private ScriptC_mandelbrot fractalRenderScript;
    private Allocation pixelBufferAllocation;
    private Allocation pixelBufferSizesAllocation;
    private Context context;

    private ArrayList<LinkedBlockingQueue<FractalComputeArguments>> renderQueueList = new ArrayList<LinkedBlockingQueue<FractalComputeArguments>>();
    private ArrayList<GPURenderThread> renderThreadList;
    private ArrayList<Boolean> rendersComplete;

    public void setContext(Context context) {
        this.context = context;
    }

    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        super.initialise(width, height, delegate);

        this.initialiseRenderThread();
        this.initialiseRenderScript();
    }

    public void initialiseRenderThread() {
        if (this.renderThreadList != null && !this.renderThreadList.isEmpty()) {
            this.stopAllRendering();
            this.interruptThreads();
        }

        this.renderThreadList = new ArrayList<GPURenderThread>();
        this.rendersComplete = new ArrayList<Boolean>();

        this.rendersComplete.add(false);
        this.renderQueueList.add(new LinkedBlockingQueue<FractalComputeArguments>());
        this.renderThreadList.add(new GPURenderThread(this, 0));
        this.renderThreadList.get(0).start();

    }

    public void interruptThreads() {
        for (GPURenderThread thread : renderThreadList) {
            thread.interrupt();
        }
    }

    private boolean initialiseRenderScript() {
        try {
            this.renderScript = RenderScript.create(this.context);
            this.fractalRenderScript = new ScriptC_mandelbrot(this.renderScript, context.getResources(), R.raw.mandelbrot);

            Log.i("GFCS", "Initialised renderscript objects successfully");
            return true;
        } catch (Throwable throwable) {
            Log.e("GFCS", "Failed to initialise renderscript: " + throwable.getLocalizedMessage());
            return false;
        }
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

        rendersComplete.set(0, false);

        Log.i("GFCS", "Scheduling renderscript render on separate thread");
        this.scheduleRendering(arguments);
    }

    private int[] buildIntArray(List<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }

    public void computeFractalWithThreadID(FractalComputeArguments arguments, int threadID) {
        if (this.renderScript == null)
            return;

        this.delegate.onComputeStarted();

        long setupStart = System.currentTimeMillis();

        int size = arguments.viewHeight * arguments.viewWidth;

        if (this.pixelBufferAllocation == null || this.pixelBufferAllocation.getType().getCount() != size) {
            if (this.pixelBufferAllocation != null)
                this.pixelBufferAllocation.destroy();
        }

        if (this.pixelBufferSizesAllocation == null || this.pixelBufferSizesAllocation.getType().getCount() != size) {
            if (this.pixelBufferSizesAllocation != null)
                this.pixelBufferSizesAllocation.destroy();
        }

        this.pixelBufferAllocation = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), size, Allocation.USAGE_SCRIPT);
        this.pixelBufferSizesAllocation = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), size, Allocation.USAGE_SCRIPT);

        int num_rows = arguments.viewHeight / arguments.pixelBlockSize;
        ArrayList<Integer> row_indices = new ArrayList<Integer>(500);

        int yStart = (arguments.viewHeight / 2);
        int yEnd = arguments.viewHeight;

        int xPixelMin = 0;
        int xPixelMax = arguments.viewWidth;
        int yPixelMin = yStart;
        int yPixelMax = yEnd;

        int imgWidth = xPixelMax - xPixelMin;
        int xPixel = 0;
        int yPixel = 0;
        int yIncrement = 0;

        int pixelIncrement = arguments.pixelBlockSize;
        int originalIncrement = pixelIncrement;

        int loopCount = 0;
        for (yIncrement = yPixelMin; yPixel < yPixelMax + (arguments.pixelBlockSize); yIncrement += pixelIncrement) {
            yPixel = yIncrement;

            pixelIncrement = (loopCount * originalIncrement);
            if (loopCount % 2 == 0) {
                pixelIncrement *= -1;
            }

            loopCount++;

            if (((imgWidth * (yPixel + arguments.pixelBlockSize - 1)) + xPixelMax) > size || yPixel < 0) {
                //rsDebug("exceeded bounds of image", 0);
                //rsDebug("yPixel", yPixel);
                //rsDebug("pixelBufferSizesLength", arraySize);
                continue;
            }


            row_indices.add(yPixel);
        }


        this.pixelBufferAllocation.copyFrom(arguments.pixelBuffer);
        this.pixelBufferSizesAllocation.copyFrom(arguments.pixelBufferSizes);

        //Log.i("GFCS", "Created renderscript allocation of size " + size);

        this.fractalRenderScript.bind_returnPixelBuffer(this.pixelBufferAllocation);
        this.fractalRenderScript.bind_returnPixelBufferSizes(this.pixelBufferSizesAllocation);

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

        this.fractalRenderScript.set_gScript(this.fractalRenderScript);

        Allocation row_indices_alloc = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), arguments.linesPerProgressUpdate, Allocation.USAGE_SCRIPT);
        int lastAllocSize = arguments.linesPerProgressUpdate;
        this.fractalRenderScript.set_gIn(row_indices_alloc);
        this.fractalRenderScript.set_gOut(row_indices_alloc);

        int numRows = row_indices.size();
        int[] primRowIndices = this.buildIntArray(row_indices);

        long setupEnd = System.currentTimeMillis();
        double setupTime = (setupEnd - setupStart) / 1000D;
        //Log.i("GFCS", "Took " + setupTime + " seconds to set up for GPU compute");

        for (int i = 0; i < numRows; i += arguments.linesPerProgressUpdate) {
            int[] indicesForUpdate = Arrays.copyOfRange(primRowIndices, i, i + arguments.linesPerProgressUpdate);
            if (indicesForUpdate.length != lastAllocSize) {
                row_indices_alloc = Allocation.createSized(this.renderScript, Element.I32(this.renderScript), indicesForUpdate.length, Allocation.USAGE_SCRIPT);
                this.fractalRenderScript.set_gIn(row_indices_alloc);
                this.fractalRenderScript.set_gOut(row_indices_alloc);
                lastAllocSize = indicesForUpdate.length;
                //Log.i("GFCS", "Created new allocation size");
            }

            row_indices_alloc.copyFrom(indicesForUpdate);
            this.fractalRenderScript.invoke_mandelbrot();

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

            if (!renderThreadList.get(0).abortSignalled())
                this.delegate.postUpdate(arguments.pixelBuffer, arguments.pixelBufferSizes);
            else
                return;
        }

        //Log.i("GFCS", "Done");
//
//        for (int i = 0; i < size; i++) {
//            if (arguments.pixelBuffer[i] != 0) {
//                Log.i("GFCS", "Not zero");
//            }
//        }
        if (!renderThreadList.get(0).abortSignalled())
            this.delegate.postFinished(arguments.pixelBuffer, arguments.pixelBufferSizes, arguments.pixelBlockSize);
    }

    void scheduleRendering(FractalComputeArguments arguments) {

        renderThreadList.get(0).allowRendering();
        renderQueueList.get(0).add(arguments);

    }

    @Override
    public boolean shouldPerformCrudeFirst() {
        return false;
    }

    @Override
    public double getIterationBase() {
        return 1.24D;
    }

    @Override
    public double getIterationConstantFactor() {
        return 54.0D;
    }

    @Override
    public double getMaxZoomLevel() {
        return -31;
    }

    @Override
    public void stopAllRendering() {
        if (!this.renderQueueList.isEmpty())
            this.renderQueueList.get(0).clear();

        if (!this.renderThreadList.isEmpty())
            this.renderThreadList.get(0).abortRendering();
    }

    public FractalComputeArguments getNextRendering(int threadID) throws InterruptedException {
        return this.renderQueueList.get(threadID).take();
    }

}
