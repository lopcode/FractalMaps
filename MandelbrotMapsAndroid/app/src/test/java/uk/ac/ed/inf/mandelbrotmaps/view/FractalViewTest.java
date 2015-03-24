package uk.ac.ed.inf.mandelbrotmaps.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.ac.ed.inf.mandelbrotmaps.BuildConfig;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class FractalViewTest {
    private FractalView view;
    private IViewResizeListener resizeListener;

    @Before
    public void setup() {
        this.resizeListener = mock(IViewResizeListener.class);
        this.view = new FractalView(RuntimeEnvironment.application, null);
        this.view.setResizeListener(this.resizeListener);
    }

    @Test
    public void testInitialisation() {
        assertTrue(this.view.isFocusable());
        assertTrue(this.view.isFocusableInTouchMode());
    }

    @Test
    public void testResizeEventFired() {
        this.view.onSizeChanged(100, 100, 0, 0);

        verify(this.resizeListener).onViewResized(this.view, 100, 100);
    }

    // TODO: Canvas related tests?
}
