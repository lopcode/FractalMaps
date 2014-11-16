package uk.ac.ed.inf.mandelbrotmaps.refactor;

import android.view.View;

public interface IFractalTouchHandler extends View.OnTouchListener, View.OnLongClickListener {
    public void setViewDelegate(IFractalTouchDelegate delegate);


}
