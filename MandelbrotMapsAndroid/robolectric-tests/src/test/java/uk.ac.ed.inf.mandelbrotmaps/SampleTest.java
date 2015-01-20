package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertNotNull;

@RunWith(uk.ac.ed.inf.mandelbrotmaps.RobolectricGradleTestRunner.class)
public class SampleTest {
    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(FractalSceneActivity.class).get();
    }

    @Test
    public void sanityTest() {
        assertNotNull(activity);
    }
}