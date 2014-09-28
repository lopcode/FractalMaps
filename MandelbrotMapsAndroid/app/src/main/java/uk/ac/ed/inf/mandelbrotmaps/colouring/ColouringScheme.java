package uk.ac.ed.inf.mandelbrotmaps.colouring;

/**
 * A colouring is used to dictate what colours the points on the screen will have.
 *
 * @author mallia
 */
public interface ColouringScheme {

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
