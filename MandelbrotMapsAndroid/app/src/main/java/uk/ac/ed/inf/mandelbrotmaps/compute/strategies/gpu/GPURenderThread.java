package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu;

import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;

public class GPURenderThread extends Thread {
    private GPUFractalComputeStrategy strategy;

    private volatile boolean abortThisRendering = false;
    public boolean isRunning = false;
    private int threadID = -1;

    public GPURenderThread(GPUFractalComputeStrategy strategy, int threadID) {
        this.strategy = strategy;
        this.threadID = threadID;
        //setPriority(Thread.MAX_PRIORITY);
    }

    public void abortRendering() {
        abortThisRendering = true;
    }

    public void allowRendering() {
        abortThisRendering = false;
    }

    public boolean abortSignalled() {
        return abortThisRendering;
    }

    public void run() {
        while (true) {
            try {
                FractalComputeArguments arguments = this.strategy.getNextRendering(threadID);
                this.strategy.computeFractalWithThreadID(arguments, threadID);
                abortThisRendering = false;
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}