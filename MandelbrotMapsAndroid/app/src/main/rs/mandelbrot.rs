#pragma version(1)
#pragma rs java_package_name(uk.ac.ed.inf.mandelbrotmaps.compute.strategies.renderscript)

#include "colouring.rsh"

rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;

// [0] is fractal, [1] is pixel buffer sizes
int *returnPixelBuffer;
int *returnPixelBufferSizes;

int pixelBlockSize;
int maxIterations;
int defaultPixelSize;
int viewWidth;
double xMin;
double yMax;
double pixelSize;
int arraySize;

double juliaX;
double juliaY;

int32_t xPixelMin;
int32_t xPixelMax;
int32_t imgWidth;

// 0 is Mandelbrot, 1 is Julia
int fractalMode;

// 0 is Purple-Red, 1 is Purple-Yellow, 2 is RGB, 3 is Pastels
int colourMode;

static int pixelInMandelbrotSet(int32_t xPixel, int32_t yPixel, double xMin, double yMax, double pixelSize, int32_t maxIterations) {
    int inside = 1;

    int32_t iterationNumber;
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
        switch(colourMode) {
            case 1:
                return colourPurpleYellowPoint(iterationNumber, maxIterations);
                break;

            case 2:
                return colourRGBPoint(iterationNumber, maxIterations);
                break;

            case 3:
                return colourPastelPoint(iterationNumber, maxIterations);
                break;

            default:
                return colourPurpleRedPoint(iterationNumber, maxIterations);
                break;
        }
    }
}

static int pixelInJuliaSet(int32_t xPixel, int32_t yPixel, double xMin, double yMax, double pixelSize, int32_t maxIterations) {
    int inside = 1;
    int iterationNumber;
    double newx, newy;
    double x, y;

    x = xMin + ((double) xPixel * pixelSize);
    y = yMax - ((double) yPixel * pixelSize);

    for (iterationNumber = 0; iterationNumber < maxIterations; iterationNumber++) {
        // z^2 + c
        newx = (x * x) - (y * y) + juliaX;
        newy = (2 * x * y) + juliaY;

        x = newx;
        y = newy;

        // Well known result: if distance is >2, escapes to infinity...
        if ((x * x + y * y) > 4) {
            inside = 0;
            break;
        }
    }

    if (inside == 1) {
        return colourInsidePoint();
    } else {
        switch(colourMode) {
            case 1:
                return colourPurpleYellowPoint(iterationNumber, maxIterations);
                break;

            case 2:
                return colourRGBPoint(iterationNumber, maxIterations);
                break;

            case 3:
                return colourPastelPoint(iterationNumber, maxIterations);
                break;

            default:
                return colourPurpleRedPoint(iterationNumber, maxIterations);
                break;
        }
    }
}

void root(const int32_t *v_in, int32_t *v_out, const void *usrData, uint32_t x, uint32_t y) {
    int32_t yPixel = *v_in;

    int xPixel = 0;
    int colourCodeHex;
    int pixelBlockA;
    int pixelBlockB;

    int *pixelBuffer = returnPixelBuffer;
    int *pixelBufferSizes = returnPixelBufferSizes;

    for (xPixel = xPixelMin; xPixel < xPixelMax + 1 - pixelBlockSize; xPixel += pixelBlockSize) {
        if (pixelBufferSizes[(imgWidth * yPixel) + xPixel] <= pixelBlockSize) {
            //rsDebug("already iterated to block size", 0);
            //rsDebug("pixelBufferSizes[(imgWidth * yPixel) + xPixel]", pixelBufferSizes[(imgWidth * yPixel) + xPixel]);
            //rsDebug("pixelBlockSize", pixelBlockSize);
            continue;
        }

        //rsDebug("inner loop", 0);
        if (fractalMode == 0) {
            colourCodeHex = pixelInMandelbrotSet(xPixel, yPixel, xMin, yMax, pixelSize, maxIterations);
        } else if(fractalMode == 1) {
            colourCodeHex = pixelInJuliaSet(xPixel, yPixel, xMin, yMax, pixelSize, maxIterations);
        }
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

void mandelbrot() {
    //rsDebug("Number of rows: ", rsAllocationGetDimX(gIn));
    imgWidth = xPixelMax - xPixelMin;
    xPixelMin = 0;
    xPixelMax = viewWidth;

    fractalMode = 0;

    rsForEach(gScript, gIn, gOut);
}

void julia() {
    //rsDebug("Number of rows: ", rsAllocationGetDimX(gIn));
    imgWidth = xPixelMax - xPixelMin;
    xPixelMin = 0;
    xPixelMax = viewWidth;

    fractalMode = 1;

    rsForEach(gScript, gIn, gOut);
}