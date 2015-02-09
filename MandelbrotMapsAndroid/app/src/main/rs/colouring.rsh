#ifndef __COLOURING_H__
#define __COLOURING_H__

static double PI = 3.1415926535;

static int colourPurpleRedPoint(int iterations, int maxIterations) {
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

static int colourPurpleYellowPoint(int iterations, int maxIterations) {
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

#define COLOUR_SPACING 30

static int colourRGBPoint(int iterations, int maxIterations) {
    if (iterations <= 0) {
        return 0xFF000000;
    }

    //number of iterations that we can handle in each segment of the colour scheme
    int maxValueForColour = 220;
    float iterationsPerSegmentDouble = ((float) maxValueForColour) / (float) COLOUR_SPACING;
    int iterationsPerSegment = (int) floor(iterationsPerSegmentDouble);

    int iterationsPerPeriod = iterationsPerSegment * 6;

    //normalise the iteration count to be between 1 and iterationsPerSegment * noOfSegments (i.e. 6)
    int exceeded = 0;


    while (iterations >= iterationsPerPeriod) {
        exceeded = 1;
        int i = iterations / iterationsPerPeriod;
        iterations = iterations - (iterationsPerPeriod * i);
    }

    //int m = 0;
    int colourCodeR = 0;
    int colourCodeG = 0;
    int colourCodeB = 0;

    //1. From Black (0,0,0) to Blue (0,0,255)
    if ((iterations < (iterationsPerSegment)) && (exceeded == 0)) {
        //the colour sequence from within the segment
        int segmentSequenceNo = iterations;
        colourCodeR = 0;
        colourCodeG = 0;
        colourCodeB = (int) (segmentSequenceNo * COLOUR_SPACING);
    }
    //7. From Magenta (255,0,255) to Blue (0,0,255)
    else if ((iterations < (iterationsPerSegment)) && (exceeded == 1)) {
        int segmentSequenceNo = iterations;
        colourCodeR = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
        colourCodeG = 0;
        colourCodeB = maxValueForColour;
    }
    //2. From Blue (0,0,255) to Cyan (0,255,255)
    else if ((iterations >= iterationsPerSegment) && (iterations < iterationsPerSegment * 2)) {
        int segmentSequenceNo = iterations - (int) iterationsPerSegment;
        colourCodeR = 0;
        colourCodeG = ((int) (segmentSequenceNo * COLOUR_SPACING));
        colourCodeB = maxValueForColour;
    }
    //3. From Cyan (0,255,255) to Green (0,255,0)
    else if ((iterations >= iterationsPerSegment * 2) && (iterations < iterationsPerSegment * 3)) {
        int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 2);
        colourCodeR = 0;
        colourCodeG = maxValueForColour;
        colourCodeB = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
    }
    //4. From Green (0,255,0) to Yellow (255,255,0)
    else if ((iterations >= iterationsPerSegment * 3) && (iterations < iterationsPerSegment * 4)) {
        int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 3);
        colourCodeR = ((int) (segmentSequenceNo * COLOUR_SPACING));
        colourCodeG = maxValueForColour;
        colourCodeB = 0;
    }
    //5. From Yellow (255,255,0) to Red (255,0,0)
    else if ((iterations >= iterationsPerSegment * 4) && (iterations < iterationsPerSegment * 5)) {
        int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 4);
        colourCodeR = maxValueForColour;
        colourCodeG = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
        colourCodeB = 0;
    }
    //6. From Red (255,0,0) to Magenta (255,0,255)
    else if ((iterations >= iterationsPerSegment * 5) && (iterations < iterationsPerSegment * 6)) {
        int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 5);
        colourCodeR = maxValueForColour;
        colourCodeG = 0;
        colourCodeB = ((int) (segmentSequenceNo * COLOUR_SPACING));
    }

    int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

    return colourCodeHex;
}

// Pastel colouring

static int boundColour(int colour, int colourRange);
static float compute_pastel_r(float theta);
static float compute_pastel_x(float theta);
static float compute_pastel_y(float theta);

static int colourPastelPoint(int iterations, int maxIterations) {
    //return black if the point escaped after 0 iterations
    if (iterations == 0) {
        return 0xFF000000;
    }

    //calucalate theta - 2pi represents 255 iterations
    float theta = (float) ((float) iterations / (float) 255) * 2 * PI;

    //compute r
    float r = compute_pastel_r(theta);

    //compute x
    float x = compute_pastel_x(theta);

    //compute y
    float y = compute_pastel_y(theta);

    //defines the number of colours used in each component of RGB
    int colourRange = 230;
    //the starting point in each compenent of RGB
    int startColour = 25;

    //compute the red compnent
    int colourCodeR = (int) (colourRange * r);
    colourCodeR = boundColour(colourCodeR, colourRange);
    colourCodeR += startColour;

    //compute the green component
    int colourCodeG = (int) (colourRange * y);
    colourCodeG = boundColour(colourCodeG, colourRange);
    colourCodeG += startColour;

    //compute the blue component
    int colourCodeB = (int) (colourRange * x);
    colourCodeB = boundColour(colourCodeB, colourRange);
    colourCodeB += startColour;

    //compute colour from the three components
    int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

    //return colour
    return colourCodeHex;
}

static int boundColour(int colour, int colourRange) {
    if (colour > (colourRange * 2)) {
        int i = (int) (colour / (colourRange * 2));

        colour = colour - (colourRange * 2 * i);
    }
    if (colour > (colourRange)) {
        colour = colourRange - (colour - colourRange);
    }

    return colour;
}

static float compute_pastel_r(float theta) {
    return theta;
}

static float compute_pastel_x(float t) {
    return t * (2.0 * (cos(t) + 1));
}

static float compute_pastel_y(float t) {
    return t * (2.0 * (sin(t) + 1));
}


#endif