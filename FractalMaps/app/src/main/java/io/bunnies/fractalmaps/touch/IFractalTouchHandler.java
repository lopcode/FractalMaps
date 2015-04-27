package io.bunnies.fractalmaps.touch;

import android.view.ScaleGestureDetector;
import android.view.View;

public interface IFractalTouchHandler extends View.OnTouchListener, View.OnLongClickListener, ScaleGestureDetector.OnScaleGestureListener {
    public void setTouchDelegate(IFractalTouchDelegate delegate);
}
