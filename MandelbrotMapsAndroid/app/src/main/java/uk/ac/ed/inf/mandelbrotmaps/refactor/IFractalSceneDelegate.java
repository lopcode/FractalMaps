package uk.ac.ed.inf.mandelbrotmaps.refactor;

public interface IFractalSceneDelegate {
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering);

    public void onFractalLongClick(IFractalPresenter presenter, float x, float y);

    public void onFractalRecomputed(IFractalPresenter presenter);

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter);
}
