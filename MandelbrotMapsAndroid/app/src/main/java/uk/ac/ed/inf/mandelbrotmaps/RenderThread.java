package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalComputeDelegate;

public class RenderThread extends Thread {
    private IFractalComputeDelegate delegate;

    private volatile boolean abortThisRendering = false;
    public boolean isRunning = false;
    private int threadID = -1;

    public RenderThread(IFractalComputeDelegate delegate, int _threadID, int _noOfThreads) {
        this.delegate = delegate;
        threadID = _threadID;
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
        return;
//        while (true) {
//            try {
//                Rendering newRendering = mjCanvas.getNextRendering(threadID);
////                mjCanvas.computeAllPixels(newRendering.getPixelBlockSize(), threadID);
//                abortThisRendering = false;
//            } catch (InterruptedException e) {
//                return;
//            }
//        }
    }
}