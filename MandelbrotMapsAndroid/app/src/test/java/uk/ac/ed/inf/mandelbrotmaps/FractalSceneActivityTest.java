package uk.ac.ed.inf.mandelbrotmaps;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@Config(emulateSdk = 18, manifest="app/src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class FractalSceneActivityTest {
    private FractalSceneActivity activity;

    @Before
    public void setup() throws Exception {
        this.activity = Robolectric.buildActivity(FractalSceneActivity.class).create().get();
    }

    @Test
    public void testActivityCreated() {
        assertNotNull(this.activity);
    }

    @Test
    public void testLayout()
    {
        assertNotNull(this.activity.findViewById(R.id.firstFractalView));
        assertNotNull(this.activity.findViewById(R.id.secondFractalView));
        assertNotNull(this.activity.findViewById(R.id.toolbar));
        assertNotNull(this.activity.findViewById(R.id.toolbarProgress));
        assertNotNull(this.activity.findViewById(R.id.toolbarTextProgress));
    }
}