package uk.ac.ed.inf.mandelbrotmaps.colouring;

public interface IColourStrategy {

    /**
     * Colours a point which escapes from the set
     *
     * @param iterations The number of iterations that the point needed to escape from the set
     * @return RGB colour as int
     */
    public int colourOutsidePoint(int iterations, int maxIterations);

    /**
     * Colours a point which is bounded to the set
     *
     * @return returns RGB colour as int
     */
    public int colourInsidePoint();
}
