package uk.ac.ed.inf.mandelbrotmaps.overlay.pin;

import android.graphics.Canvas;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import uk.ac.ed.inf.mandelbrotmaps.BuildConfig;
import uk.ac.ed.inf.mandelbrotmaps.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class PinOverlayTest {
    private PinOverlay pinOverlay;

    @Before
    public void setup() {
        this.pinOverlay = new PinOverlay(RuntimeEnvironment.application, 1.0f, 1f, 2f);
    }

    @Test
    public void testInitialisation() {
        assertEquals(this.pinOverlay.getX(), 1f, 0.1f);
        assertEquals(this.pinOverlay.getY(), 2f, 0.1f);
    }

    @Test
    public void testSetPosition() {
        this.pinOverlay.setPosition(4f, 5f);
        assertEquals(this.pinOverlay.getX(), 4f, 0.1f);
        assertEquals(this.pinOverlay.getY(), 5f, 0.1f);
        assertEquals(this.pinOverlay.getPinRadius(), 1f, 0.1f);
    }

    @Test
    public void testAssertTransformed() {
        assertTrue(this.pinOverlay.isAffectedByTransform());
    }

    @Test
    public void testSetPinRadius() {
        this.pinOverlay.setPinRadius(123f);
        assertEquals(this.pinOverlay.getPinRadius(), 123f, 0.1f);
    }

    @Test
    public void testSetPinColour() {
        this.pinOverlay.setPinColour(PinColour.MAGENTA);
        assertEquals(this.pinOverlay.getPinInnerPaint().getColor(), RuntimeEnvironment.application.getResources().getColor(R.color.dark_purple));
        assertEquals(this.pinOverlay.getPinOuterPaint().getColor(), RuntimeEnvironment.application.getResources().getColor(R.color.purple));
    }

    @Test
    @Ignore // Fails to mock Canvas in RL 3.0-SNAPSHOT
    public void testLabelDrawsToCanvas() {
        Canvas canvas = mock(Canvas.class);

        this.pinOverlay.drawToCanvas(canvas, 3f, 4f);

        verify(canvas).drawCircle(3f, 4f, 1.0f, this.pinOverlay.getPinInnerPaint());
        verify(canvas).drawCircle(3f, 4f, 1.0f * 0.5f, this.pinOverlay.getPinOuterPaint());
    }

    @Test
    @Ignore // Fails to mock Canvas in RL 3.0-SNAPSHOT
    public void testLabelDrawsToCanvasWithDifferingHilightState() {
        Canvas canvas = mock(Canvas.class);

        this.pinOverlay.setHilighted(true);
        this.pinOverlay.drawToCanvas(canvas, 3f, 4f);

        verify(canvas).drawCircle(3f, 4f, 1.0f * 2, this.pinOverlay.getPinInnerPaint());
        verify(canvas).drawCircle(3f, 4f, 1.0f, this.pinOverlay.getPinOuterPaint());
    }
}
