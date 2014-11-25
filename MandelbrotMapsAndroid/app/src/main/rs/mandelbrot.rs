#pragma version(1)
#pragma rs java_package_name(uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu)

// [0] is fractal, [1] is pixel buffer sizes
int *returnPixelBuffer;
int *returnPixelBufferSizes;

static double PI = 3.1415926535;

static int colourOutsidePoint(int iterations, int maxIterations) {
        if (iterations <= 0) {
            return 0xFF000000;
        }

        int colourCodeR, colourCodeG, colourCodeB;
        double colourCode;

        // Percentage (0.0 -- 1.0)
        colourCode = (double) iterations / (double) maxIterations;

        // Red
        colourCodeR = (int) (255 * 6 * colourCode);
        if (255 < colourCodeR) {
            colourCodeR = 255;
        }

        // Green
        colourCodeG = (int) (255 * colourCode);

        float c = 7 * PI * colourCode;
        // Blue
        colourCodeB = (int) (127.5 - 127.5 * cos(c));

        //Compute colour from the three components
        int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

        return colourCodeHex;
    }

static int colourInsidePoint() {
    return 0xFFFFFFFF;
}

static int pixelInMandelbrotSet(int xPixel, int yPixel, double xMin, double yMax, double pixelSize, int maxIterations) {
    int inside = 1;

    int iterationNumber;
    double newx;
    double newy;
    double x;
    double y;

    double x0 = xMin + ((double) xPixel * pixelSize);
    double y0 = yMax - ((double) yPixel * pixelSize);

    x = x0;
    y = y0;

    for (iterationNumber = 0; iterationNumber < maxIterations; iterationNumber++) {
        newx = (x * x) - (y * y) + x0;
        newy = (2 * x * y) + y0;

        x = newx;
        y = newy;

        if ((x * x + y * y) > 4) {
            inside = 0;
            break;
        }
    }

    if (inside == 1) {
        return colourInsidePoint();
    } else {
        return colourOutsidePoint(iterationNumber, maxIterations);
    }
}

void mandelbrot(int pixelBlockSize, int maxIterations, int defaultPixelSize,
                int viewWidth, int viewHeight, double xMin, double yMax,
                double pixelSize, int arraySize) {
    int numberOfThreads = 1;
    int threadID = 0;

    int *pixelBuffer = returnPixelBuffer;
    int *pixelBufferSizes = returnPixelBufferSizes;

    int yStart = (viewHeight / 2);
    int yEnd = viewHeight - (numberOfThreads - 1);

    int xPixelMin = 0;
    int xPixelMax = viewWidth;
    int yPixelMin = yStart;
    int yPixelMax = yEnd;

    int imgWidth = xPixelMax - xPixelMin;
    int xPixel = 0;
    int yPixel = 0;
    int yIncrement = 0;
    int colourCodeHex;
    int pixelBlockA;
    int pixelBlockB;

    double x0 = 0;
    double y0 = 0;

    int pixelIncrement = pixelBlockSize * numberOfThreads;
    int originalIncrement = pixelIncrement;

    int loopCount = 0;

    //rsDebug("starting loop", 0);

    for (yIncrement = yPixelMin; yPixel < yPixelMax + (numberOfThreads * pixelBlockSize); yIncrement += pixelIncrement) {
        yPixel = yIncrement;

        //rsDebug("y loop", yIncrement);

        pixelIncrement = (loopCount * originalIncrement);
        if (loopCount % 2 == 0) {
            pixelIncrement *= -1;
        }

        loopCount++;

        if (((imgWidth * (yPixel + pixelBlockSize - 1)) + xPixelMax) > arraySize || yPixel < 0) {
            //rsDebug("exceeded bounds of image", 0);
            //rsDebug("yPixel", yPixel);
            //rsDebug("pixelBufferSizesLength", arraySize);
            continue;
        }

        for (xPixel = xPixelMin; xPixel < xPixelMax + 1 - pixelBlockSize; xPixel += pixelBlockSize) {
            if (pixelBufferSizes[(imgWidth * yPixel) + xPixel] <= pixelBlockSize) {
                //rsDebug("already iterated to block size", 0);
                //rsDebug("pixelBufferSizes[(imgWidth * yPixel) + xPixel]", pixelBufferSizes[(imgWidth * yPixel) + xPixel]);
                //rsDebug("pixelBlockSize", pixelBlockSize);
                continue;
            }

            //rsDebug("inner loop", 0);
            colourCodeHex = pixelInMandelbrotSet(xPixel, yPixel, xMin, yMax, pixelSize, maxIterations);
            //if (value != 0) {
            //    rsDebug("value", value);
            //}

            pixelBufferSizes[(imgWidth * yPixel) + (xPixel)] = defaultPixelSize;

            int p = 0;
            for (pixelBlockA = 0; pixelBlockA < pixelBlockSize; pixelBlockA++) {
                for (pixelBlockB = 0; pixelBlockB < pixelBlockSize; pixelBlockB++) {
                    if (p != 0) {
                        pixelBufferSizes[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = pixelBlockSize;
                    }
                    p++;
                    pixelBuffer[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = colourCodeHex;
                }
            }
        }
    }

    returnPixelBuffer = pixelBuffer;
    returnPixelBufferSizes = pixelBufferSizes;
}