package uk.ac.ed.inf.mandelbrotmaps.overlay.label;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import uk.ac.ed.inf.mandelbrotmaps.BuildConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class LabelOverlayTest {
    private LabelOverlay labelOverlay;

    @Before
    public void setup() {
        this.labelOverlay = new LabelOverlay(RuntimeEnvironment.application, "Initial text", 1f, 2f);
    }

    @Test
    public void testInitialisation() {
        assertEquals(this.labelOverlay.getText(), "Initial text");
        assertEquals(this.labelOverlay.getX(), 1f, 0.1f);
        assertEquals(this.labelOverlay.getY(), 2f, 0.1f);
    }

    @Test
    public void testSetPosition() {
        this.labelOverlay.setPosition(4f, 5f);
        assertEquals(this.labelOverlay.getX(), 4f, 0.1f);
        assertEquals(this.labelOverlay.getY(), 5f, 0.1f);
    }

    @Test
    public void testNotTransformed() {
        assertFalse(this.labelOverlay.isAffectedByTransform());
    }

    @Test
    @Ignore // Fails to mock Canvas in RL 3.0-SNAPSHOT
    public void testLabelDrawsToCanvas() {
        Canvas canvas = mock(Canvas.class);
        Paint paint = mock(Paint.class);

        this.labelOverlay.drawToCanvas(canvas, 3f, 4f);

        verify(canvas).drawText("Initial text", 3f, 4f, paint);
    }
}
