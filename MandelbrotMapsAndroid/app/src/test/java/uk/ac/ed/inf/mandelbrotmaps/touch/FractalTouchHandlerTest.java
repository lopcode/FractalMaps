package uk.ac.ed.inf.mandelbrotmaps.touch;

import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import uk.ac.ed.inf.mandelbrotmaps.BuildConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class FractalTouchHandlerTest {
    private FractalTouchHandler touchHandler;
    private IFractalTouchDelegate touchDelegate;

    @Before
    public void setup() {
        this.touchDelegate = mock(IFractalTouchDelegate.class);
        this.touchHandler = new FractalTouchHandler(RuntimeEnvironment.application, this.touchDelegate);
    }

    @Test
    public void testInitialisation() {
        assertEquals(this.touchHandler.getTotalDragX(), 0, 0.1f);
        assertEquals(this.touchHandler.getTotalDragY(), 0, 0.1f);

        assertFalse(this.touchHandler.isCurrentlyDragging());
        assertFalse(this.touchHandler.isCurrentlyScaling());

        assertEquals(this.touchHandler.getCurrentScaleFactor(), 0, 0.1f);
    }

    @Test
    public void testStartDraggingFractal() {
        this.touchHandler.startDraggingFractal(1f, 1f, 0);

        assertEquals(this.touchHandler.getTotalDragX(), 0, 0.1f);
        assertEquals(this.touchHandler.getTotalDragY(), 0, 0.1f);

        assertEquals(this.touchHandler.getCurrentPointerID(), 0);
        assertTrue(this.touchHandler.isCurrentlyDragging());

        verify(this.touchDelegate).startDraggingFractal();
    }

    @Test
    public void testDragFractal() {
        this.touchHandler.startDraggingFractal(0f, 0f, 0);
        this.touchHandler.dragFractal(2f, 3f);

        verify(this.touchDelegate).dragFractal(2f, 3f, 2f, 3f);
    }

    @Test
    public void testStopDraggingFractal() {
        this.touchHandler.startDraggingFractal(0f, 0f, 0);
        this.touchHandler.dragFractal(2f, 3f);
        this.touchHandler.stopDraggingFractal();

        verify(this.touchDelegate).stopDraggingFractal(false, 2f, 3f);
    }

    @Test
    public void testStartScalingFractal() {
        this.touchHandler.startScalingFractal(1f, 2f);

        assertEquals(this.touchHandler.getCurrentScaleFactor(), 0, 0.1f);
        assertTrue(this.touchHandler.isCurrentlyScaling());

        verify(this.touchDelegate).startScalingFractal(1f, 2f);
    }

    @Test
    public void testScaleFractal() {
        this.touchHandler.startScalingFractal(1f, 2f);
        this.touchHandler.scaleFractal(1f, 2f, 2.0f);

        verify(this.touchDelegate).scaleFractal(2.0f, 1f, 2f);
    }

    @Test
    public void testStopScalingFractal() {
        this.touchHandler.startScalingFractal(1f, 2f);
        this.touchHandler.scaleFractal(1f, 2f, 2.0f);
        this.touchHandler.stopScalingFractal();

        verify(this.touchDelegate).stopScalingFractal();
    }

    @Test
    public void testTouchDownEvent() {
        MotionEvent event = MotionEvent.obtain(1L, 1L, MotionEvent.ACTION_DOWN, 1f, 2f, 0);
        View view = mock(View.class);

        FractalTouchHandler touchHandlerSpied = spy(this.touchHandler);
        touchHandlerSpied.onTouch(view, event);

        verify(touchHandlerSpied).onTouchDown(1f, 2f, 0, 1);
    }

    @Test
    public void testTouchMoveEvent() {
        MotionEvent eventDown = MotionEvent.obtain(1L, 1L, MotionEvent.ACTION_DOWN, 1f, 2f, 0);
        MotionEvent eventMove = MotionEvent.obtain(1L, 1L, MotionEvent.ACTION_MOVE, 3f, 4f, 0);
        View view = mock(View.class);

        FractalTouchHandler touchHandlerSpied = spy(this.touchHandler);
        touchHandlerSpied.onTouch(view, eventDown);
        touchHandlerSpied.onTouch(view, eventMove);

        verify(touchHandlerSpied).onTouchMove(3f, 4f, 1);
    }

    @Test
    public void testTouchUpEvent() {
        MotionEvent eventDown = MotionEvent.obtain(1L, 1L, MotionEvent.ACTION_DOWN, 1f, 2f, 0);
        MotionEvent eventUp = MotionEvent.obtain(1L, 1L, MotionEvent.ACTION_UP, 5f, 6f, 0);
        View view = mock(View.class);

        FractalTouchHandler touchHandlerSpied = spy(this.touchHandler);
        touchHandlerSpied.onTouch(view, eventDown);
        touchHandlerSpied.onTouch(view, eventUp);

        verify(touchHandlerSpied).onTouchUp(5f, 6f);
    }
}
