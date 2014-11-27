package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.cpu;

import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;

public class CPURenderThread extends Thread {
    private CPUFractalComputeStrategy strategy;

    private volatile boolean abortThisRendering = false;
    public boolean isRunning = false;
    private int threadID = -1;

    public CPURenderThread(CPUFractalComputeStrategy strategy, int threadID) {
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