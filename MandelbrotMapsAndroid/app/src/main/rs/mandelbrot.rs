#pragma version(1)
#pragma rs java_package_name(uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu)

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

int mandelbrotMode;
int juliaMode;

static double PI = 3.1415926535;

static int colourOutsideMandelbrotPoint(int iterations, int maxIterations) {
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

static int colourInsideMandelbrotPoint() {
    return 0xFFFFFFFF;
}

static int colourOutsideJuliaPoint(int iterations, int maxIterations) {
    if (iterations <= 0) {
        return 0xFF000000;
    }

    int colourCodeR, colourCodeG, colourCodeB;
    double colourCode;


    // Percentage (0.0 -- 1.0)
    colourCode = (double) iterations / (double) maxIterations;

    // Red
    colourCodeR = (int) (255 * 2 * colourCode);
    if (255 < colourCodeR) {
        colourCodeR = 255;
    }

    // Green
    colourCodeG = (int) (255 * colourCode);

    // Blue
    float c = 3 * PI * colourCode;
    colourCodeB = (int) (
            127.5 - 127.5 * cos(c)
    );

    //Compute colour from the three components
    int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

    return colourCodeHex;
}

static int colourInsideJuliaPoint() {
    return 0xFFFFFFFF;
}


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
        return colourInsideMandelbrotPoint();
    } else {
        return colourOutsideMandelbrotPoint(iterationNumber, maxIterations);
    }
}

static int pixelInJuliaSet(int32_t xPixel, int32_t yPixel, double xMin, double yMax, double pixelSize, int32_t maxIterations) {
    int inside = 1;
    int iterationNr;
    double newx, newy;
    double x, y;

    x = xMin + ((double) xPixel * pixelSize);
    y = yMax - ((double) yPixel * pixelSize);

    for (iterationNr = 0; iterationNr < maxIterations; iterationNr++) {
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
        return colourInsideJuliaPoint();
    } else {
        return colourOutsideJuliaPoint(iterationNr, maxIterations);
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
        if (mandelbrotMode == 1) {
            colourCodeHex = pixelInMandelbrotSet(xPixel, yPixel, xMin, yMax, pixelSize, maxIterations);
        } else if(juliaMode == 1) {
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

    mandelbrotMode = 1;
    juliaMode = 0;

    rsForEach(gScript, gIn, gOut);
}

void julia() {
    //rsDebug("Number of rows: ", rsAllocationGetDimX(gIn));
    imgWidth = xPixelMax - xPixelMin;
    xPixelMin = 0;
    xPixelMax = viewWidth;

    mandelbrotMode = 0;
    juliaMode = 1;

    rsForEach(gScript, gIn, gOut);
}