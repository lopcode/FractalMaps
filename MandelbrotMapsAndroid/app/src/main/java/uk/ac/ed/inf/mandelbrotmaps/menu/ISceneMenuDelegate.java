package uk.ac.ed.inf.mandelbrotmaps.menu;

public interface ISceneMenuDelegate {
    public void onResetAllClicked();

    public void onHelpClicked();

    public void onShowMandelbrotMenuClicked();

    public void onShowJuliaMenuClicked();

    public void onSettingsClicked();

    // Benchmarking

    public void onBenchmarkOldClicked();

    public void onBenchmarkOneClicked();

    public void onBenchmarkTwoClicked();

    public void onBenchmarkThreeClicked();

    public void onBenchmarkFourClicked();

    public void onBenchmarkFiveClicked();

    public void onBenchmarkSixClicked();

    public void onBenchmarkSevenClicked();
}
