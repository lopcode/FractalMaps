package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.renderscript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;

public class RenderscriptRenderThread extends Thread {
    private final Logger LOGGER = LoggerFactory.getLogger(RenderscriptRenderThread.class);

    private RenderscriptFractalComputeStrategy strategy;
    private final Boolean readyLock = false;
    private boolean waitingForRender = false;
    private volatile boolean isStopped = false;

    public RenderscriptRenderThread(RenderscriptFractalComputeStrategy strategy) {
        this.strategy = strategy;
        //setPriority(Thread.MAX_PRIORITY);
    }

    public synchronized void stopRendering() {
        LOGGER.debug("Aborting render...");

        synchronized (this.readyLock) {
            if (!this.waitingForRender) {
                try {
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    //Log.i("RRT", "Waiting for ready lock");
                    this.isStopped = true;
                    this.readyLock.wait();

                } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                }
            }
        }

        this.isStopped = false;

        //Log.i("RRT", "Ready lock released");
    }

    public void allowRendering() {
        //Log.i("RRT", "Rendering allowed to continue");

        this.isStopped = false;
    }

    public boolean abortSignalled() {
        //Log.i("RRT", "Is abort signalled: " + this.isStopped);

        return this.isStopped;
    }

    public void run() {
        while (true) {
            try {
                this.allowRendering();

                synchronized (readyLock) {
                    //Log.i("RRT", "Waiting for render...");
                    this.waitingForRender = true;
                }

                FractalComputeArguments arguments = this.strategy.getNextRendering();

                synchronized (readyLock) {
                    //Log.i("RRT", "Not waiting for render");
                    this.waitingForRender = false;
                }

                arguments.startTime = System.nanoTime();

                if (!(this.strategy == null || this.abortSignalled() || this.strategy.getContext() == null)) {
                    this.strategy.computeFractalWithThreadID(arguments);
                }

                synchronized (readyLock) {
                    //Log.i("RRT", "Notifying ready lock");
                    readyLock.notify();
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}