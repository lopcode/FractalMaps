package uk.ac.ed.inf.mandelbrotmaps;


class MandelbrotJuliaLocation {
	//private URL urlBase;
	private double[] mandelbrotGraphArea;
	private double[] juliaGraphArea;
	private double[] juliaParam;
	private int mandelbrotContrast = -1;
	private int juliaContrast = -1;
	
	// Constructor. Defaults - some semi-arbitrary, pretty values
	public MandelbrotJuliaLocation() {
		mandelbrotGraphArea = new double[] {-2.5, 1.15, 4};
		juliaGraphArea = new double[] {-1.8, 1.45, 3.6};
		juliaParam = new double[] {0.152, 0.584};
	}
	
	public double[] getMandelbrotGraphArea() {
		return mandelbrotGraphArea;
	}
	
	public double[] getJuliaGraphArea() {
		return juliaGraphArea;
	}
	
	public double[] getJuliaParam() {
		return juliaParam;
	}
	
	public int getMandelbrotContrast() {
		return mandelbrotContrast;
	}
	
	public int getJuliaContrast() {
		return juliaContrast;
	}
}