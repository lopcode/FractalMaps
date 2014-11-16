package uk.ac.ed.inf.mandelbrotmaps.refactor;

import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.IFractalComputeStrategy;

public class JuliaPresenter extends FractalPresenter {
    private double[] juliaParams;

    public JuliaPresenter(IFractalComputeStrategy fractalStrategy) {
        super(fractalStrategy);
    }
}
