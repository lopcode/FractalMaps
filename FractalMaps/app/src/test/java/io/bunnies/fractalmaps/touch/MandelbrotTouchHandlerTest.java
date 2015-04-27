package io.bunnies.fractalmaps.touch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.bunnies.fractalmaps.BuildConfig;
import io.bunnies.fractalmaps.overlay.pin.IPinMovementDelegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class MandelbrotTouchHandlerTest {
    private MandelbrotTouchHandler touchHandler;
    private IFractalTouchDelegate touchDelegate;
    private IPinMovementDelegate pinMovementDelegate;

    @Before
    public void setup() {
        this.touchDelegate = mock(IFractalTouchDelegate.class);
        this.pinMovementDelegate = mock(IPinMovementDelegate.class);

        this.touchHandler = new MandelbrotTouchHandler(RuntimeEnvironment.application, this.touchDelegate);
        this.touchHandler.setPinMovementDelegate(this.pinMovementDelegate);
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
    public void testIsTouchingPin() {
        stub(this.pinMovementDelegate.getPinX()).toReturn(105f);
        stub(this.pinMovementDelegate.getPinY()).toReturn(106f);
        stub(this.pinMovementDelegate.getPinRadius()).toReturn(2f);

        assertTrue(this.touchHandler.isTouchingPin(106f, 107f));
        assertFalse(this.touchHandler.isTouchingPin(110f, 111f));
    }

    @Test
    public void testStartDraggingPin() {
        stub(this.pinMovementDelegate.getPinX()).toReturn(105f);
        stub(this.pinMovementDelegate.getPinY()).toReturn(106f);
        stub(this.pinMovementDelegate.getPinRadius()).toReturn(2f);

        this.touchHandler.onTouchDown(105f, 106f, 0, 1);

        verify(this.pinMovementDelegate).startedDraggingPin();
    }

    @Test
    public void testDragPin() {
        stub(this.pinMovementDelegate.getPinX()).toReturn(105f);
        stub(this.pinMovementDelegate.getPinY()).toReturn(106f);
        stub(this.pinMovementDelegate.getPinRadius()).toReturn(2f);

        this.touchHandler.onTouchDown(105f, 106f, 0, 1);
        this.touchHandler.onTouchMove(110f, 115f, 1);

        verify(this.pinMovementDelegate).startedDraggingPin();
        verify(this.pinMovementDelegate).pinDragged(110f, 115f, false);
    }

    @Test
    public void testStopDraggingPin() {
        stub(this.pinMovementDelegate.getPinX()).toReturn(105f);
        stub(this.pinMovementDelegate.getPinY()).toReturn(106f);
        stub(this.pinMovementDelegate.getPinRadius()).toReturn(2f);

        this.touchHandler.onTouchDown(105f, 106f, 0, 1);
        this.touchHandler.onTouchMove(110f, 115f, 1);
        this.touchHandler.onTouchUp(115f, 120f);

        verify(this.pinMovementDelegate).startedDraggingPin();
        verify(this.pinMovementDelegate).pinDragged(110f, 115f, false);
        verify(this.pinMovementDelegate).stoppedDraggingPin(115f, 120f);
    }
}
