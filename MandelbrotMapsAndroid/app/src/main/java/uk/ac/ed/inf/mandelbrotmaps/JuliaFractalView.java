package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.JuliaCPUFractalComputeStrategy;

public class JuliaFractalView extends AbstractFractalView {
    // Point paramaterising this Julia set
    private double juliaX = 0;
    private double juliaY = 0;

    public JuliaFractalView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void initialise(FractalActivity parentActivity) {
        super.initialise(parentActivity);

        setColouringScheme(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("JULIA_COLOURS", "JuliaDefault"), false);

        for (int i = 0; i < noOfThreads; i++) {
            renderThreadList.get(i).setName("Julia thread " + i);
        }

        // How deep a zoom do we allow?
        MAXZOOM_LN_PIXEL = -20; // Beyond -21, "double"s break down(!).
    }

    public void setJuliaParameter(double newJuliaX, double newJuliaY) {
        //stopAllRendering();
        juliaX = newJuliaX;
        juliaY = newJuliaY;

        ((JuliaCPUFractalComputeStrategy) this.strategy).setJuliaSeed(juliaX, juliaY);

        setGraphArea(graphArea, true);
    }

    public double[] getJuliaParam() {
        double[] juliaParam = new double[2];
        juliaParam[0] = juliaX;
        juliaParam[1] = juliaY;
        return juliaParam;
    }

    // Load a location
    void loadLocation(MandelbrotJuliaLocation mjLocation) {
        //setScaledIterationCount(mjLocation.getJuliaContrast());
        double[] juliaParam = mjLocation.getJuliaParam();
        setGraphArea(mjLocation.getJuliaGraphArea(), true);

        setJuliaParameter(juliaParam[0], juliaParam[1]);

        //mandelbrotStrategy.setGraphArea(mjLocation.getJuliaGraphArea());
    }
}
