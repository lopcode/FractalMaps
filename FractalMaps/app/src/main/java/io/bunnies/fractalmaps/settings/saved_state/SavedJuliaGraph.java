package io.bunnies.fractalmaps.settings.saved_state;

public class SavedJuliaGraph extends SavedGraphArea {
    public double juliaSeedX;
    public double juliaSeedY;

    public SavedJuliaGraph(double graphX, double graphY, double graphZ, double juliaSeedX, double juliaSeedY) {
        super(graphX, graphY, graphZ);
        this.juliaSeedX = juliaSeedX;
        this.juliaSeedY = juliaSeedY;
    }
}
