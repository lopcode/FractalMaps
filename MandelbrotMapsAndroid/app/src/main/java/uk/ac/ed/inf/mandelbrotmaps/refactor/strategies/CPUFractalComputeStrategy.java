package uk.ac.ed.inf.mandelbrotmaps.refactor.strategies;

public abstract class CPUFractalComputeStrategy extends FractalComputeStrategy {

    @Override
    public void computeFractal(int pixelBlockSize, boolean showRenderingProgress, int maxIterations, int linesPerProgressUpdate, int defaultPixelSize, int xPixelMin, int xPixelMax, int yPixelMin, int yPixelMax, double xMin, double yMax, double pixelSize, int[] pixelBuffer, int[] pixelBufferSizes) {
        int imgWidth = xPixelMax - xPixelMin;
        int noOfThreads = 1;

        int xPixel = 0, yPixel = 0, yIncrement = 0;
        int colourCodeHex;
        int pixelBlockA, pixelBlockB;

        this.xMin = xMin;
        this.yMax = yMax;
        this.pixelSize = pixelSize;

        double x0 = 0, y0 = 0;

        int pixelIncrement = pixelBlockSize * noOfThreads;
        int originalIncrement = pixelIncrement;

        int loopCount = 0;


        for (yIncrement = yPixelMin; yPixel < yPixelMax + (noOfThreads * pixelBlockSize); yIncrement += pixelIncrement) {
            yPixel = yIncrement;

            pixelIncrement = (loopCount * originalIncrement);
            if (loopCount % 2 == 0)
                pixelIncrement *= -1;
            loopCount++;

            //If we've exceeded the bounds of the image (as can happen with many threads), exit the loop.
            if (((imgWidth * (yPixel + pixelBlockSize - 1)) + xPixelMax) > pixelBufferSizes.length ||
                    yPixel < 0) {
                int q = 1;
                continue;
            }

//            // Detect rendering abortion.
//            if (allowInterruption && (callingThread.abortSignalled())) {
//                return;
//            }

            // Set y0 (im part of c)
            //y0 = yMax - ( (double)yPixel * pixelSize );

            for (xPixel = xPixelMin; xPixel < xPixelMax + 1 - pixelBlockSize; xPixel += pixelBlockSize) {
                //Check to see if this pixel is already iterated to the necessary block size

                if (pixelBufferSizes[(imgWidth * yPixel) + xPixel] <= pixelBlockSize) {
                    continue;
                }

                colourCodeHex = pixelInSet(xPixel, yPixel, maxIterations);

                //Note that the pixel being calculated has been calculated in full (upper right of a block)
//                if (fractalViewSize == FractalViewSize.LARGE)
                pixelBufferSizes[(imgWidth * yPixel) + (xPixel)] = defaultPixelSize;

                // Save colour info for this pixel. int, interpreted: 0xAARRGGBB
                int p = 0;
                for (pixelBlockA = 0; pixelBlockA < pixelBlockSize; pixelBlockA++) {
                    for (pixelBlockB = 0; pixelBlockB < pixelBlockSize; pixelBlockB++) {
//                        if (fractalViewSize == FractalViewSize.LARGE) {
                        if (p != 0) {
                            pixelBufferSizes[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = pixelBlockSize;
                        }
                        p++;
//                        }
                        if (pixelBuffer == null) return;
                        pixelBuffer[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = colourCodeHex;
                    }
                }
            }
            // Show thread's work in progress
            if ((showRenderingProgress) && (loopCount % linesPerProgressUpdate == 0)) {
                this.delegate.postUpdate(pixelBuffer, pixelBufferSizes);

            }
        }

        this.delegate.postFinished(pixelBuffer, pixelBuffer, pixelBlockSize);
    }
}
