package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.renderscript;

import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;

public class RenderscriptRenderThread extends Thread {
    private RenderscriptFractalComputeStrategy strategy;

    private volatile boolean abortThisRendering = false;
    public boolean isRunning = false;
    private int threadID = -1;

    public RenderscriptRenderThread(RenderscriptFractalComputeStrategy strategy, int threadID) {
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
                arguments.startTime = System.nanoTime();

                if (this.strategy == null || this.abortSignalled())
                    return;

                if (this.strategy.getContext() == null)
                    return;

                this.strategy.computeFractalWithThreadID(arguments, threadID);
                abortThisRendering = false;
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}